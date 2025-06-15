/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.core.rpc.netty.http.http2;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import org.apache.seata.common.rpc.http.HttpContext;
import org.apache.seata.common.thread.NamedThreadFactory;
import org.apache.seata.core.rpc.netty.NettyServerConfig;
import org.apache.seata.core.rpc.netty.http.ControllerManager;
import org.apache.seata.core.rpc.netty.http.HttpInvocation;
import org.apache.seata.core.rpc.netty.http.HttpThreadPoolFactory;
import org.apache.seata.core.rpc.netty.http.ParameterParser;
import org.apache.seata.core.rpc.netty.http.grpc.GrpcHeaderEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;

/**
 * A Netty HTTP request handler that dispatches incoming requests to corresponding controller methods
 */
public class Http2DispatchHandler extends ChannelDuplexHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Http2DispatchHandler.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final AtomicBoolean headerSent = new AtomicBoolean(false);
    private Http2HeadersFrame http2HeadersFrame;
    /**
     * HTTP request processing thread pool, independent of Netty IO threads, to avoid blocking network processing.
     */
    private static final ExecutorService HTTP_HANDLER_THREADS = HttpThreadPoolFactory.getHttpHandlerThreads();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof Http2HeadersFrame) {
            http2HeadersFrame = (Http2HeadersFrame) msg;
            if (http2HeadersFrame.isEndStream()) {
               sendResponse(ctx);
            }
        } else if (msg instanceof Http2DataFrame) {
            String path = http2HeadersFrame.headers().path().toString();
            QueryStringDecoder queryStringDecoder = new QueryStringDecoder(path);
            ObjectNode requestDataNode = OBJECT_MAPPER.createObjectNode();
            requestDataNode.putIfAbsent("param", ParameterParser.convertParamMap(queryStringDecoder.parameters()));
            Http2DataFrame http2DataFrame = (Http2DataFrame) msg;
            ByteBuf byteBuf = http2DataFrame.content();
            byte[] bytes = new byte[byteBuf.readableBytes()];
            byteBuf.readBytes(bytes);
            try {
                requestDataNode.putIfAbsent("body", OBJECT_MAPPER.readTree(bytes));
                HttpInvocation httpInvocation = ControllerManager.getHttpInvocation(path);

                if (httpInvocation == null) {
                    sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND);
                    return;
                }
                Http2Request httpRequest = new Http2Request(http2HeadersFrame,http2DataFrame);
                HttpContext<Http2Request> httpContext = new HttpContext<>(httpRequest, ctx, true);
                Object httpController = httpInvocation.getController();
                Method handleMethod = httpInvocation.getMethod();
                Object[] args = ParameterParser.getArgValues(httpInvocation.getParamMetaData(), handleMethod,
                        requestDataNode, httpContext);

                try {
                    HTTP_HANDLER_THREADS.execute(() -> {
                        try {
                            Object result = handleMethod.invoke(httpController, args);
                            if (httpContext.isAsync()) {
                                return;
                            }

                            sendResponse(ctx, result);
                        } catch (IllegalArgumentException e) {
                            LOGGER.error("Illegal argument exception: {}", e.getMessage(), e);
                            sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST);
                        } catch (Exception e) {
                            LOGGER.error("Exception occurred while processing HTTP request: {}", e.getMessage(), e);
                            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
                        }
                    });
                } catch (RejectedExecutionException e) {
                    LOGGER.error("HTTP thread pool is full: {}", e.getMessage(), e);
                    sendErrorResponse(ctx, HttpResponseStatus.SERVICE_UNAVAILABLE);
                }
            } catch (Exception e) {
                LOGGER.error("Exception occurred while processing HTTP request: {}", e.getMessage(), e);
                sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
            }
        }
    }



    private void sendResponse(ChannelHandlerContext ctx, Object result) throws JsonProcessingException {
        sendResponse(ctx,HttpResponseStatus.OK,result);
    }

    private void sendResponse(ChannelHandlerContext ctx) {
        try {
            sendResponse(ctx,HttpResponseStatus.OK,null);
        } catch (JsonProcessingException e) {
            LOGGER.error(e.getMessage(),e);
        }
    }

    private void sendErrorResponse(ChannelHandlerContext ctx, HttpResponseStatus status) {
        try {
            sendResponse(ctx,status,null);
        } catch (JsonProcessingException e) {
            LOGGER.error(e.getMessage(),e);
        }
    }

    private void sendResponse(ChannelHandlerContext ctx, HttpResponseStatus status,Object result)
            throws JsonProcessingException {
        if (headerSent.compareAndSet(false, true)) {
            Http2Headers headers = new DefaultHttp2Headers();
            headers.add(GrpcHeaderEnum.HTTP2_STATUS.header, status.codeAsText());
            ctx.writeAndFlush(new DefaultHttp2HeadersFrame(headers, false));
        }
        if (result != null) {
            ctx.writeAndFlush(new DefaultHttp2DataFrame(Unpooled.wrappedBuffer(OBJECT_MAPPER.writeValueAsBytes(result)), true));
        } else {
            ctx.writeAndFlush(new DefaultHttp2DataFrame(Unpooled.wrappedBuffer(Unpooled.EMPTY_BUFFER)));
        }
    }
}

