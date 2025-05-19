package org.apache.seata.core.rpc.netty.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import java.lang.reflect.Method;

public class HttpDispatchHandler extends SimpleChannelInboundHandler<HttpRequest> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpRequest httpRequest) throws Exception {
        final boolean keepAlive = HttpUtil.isKeepAlive(httpRequest)
                && httpRequest.protocolVersion().isKeepAliveDefault();
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(httpRequest.uri());
        String path = queryStringDecoder.path();

        HttpInvocation httpInvocation = ControllerManager.getHttpInvocation(path);
        if (httpInvocation == null) {
            sendNotFound(ctx, keepAlive);
            return;
        }

        ObjectNode requestDataNode = OBJECT_MAPPER.createObjectNode();
        requestDataNode.putIfAbsent("param", ParameterParser.convertParamMap(queryStringDecoder.parameters()));
        requestDataNode.putPOJO("channel", ctx.channel());

        if (httpRequest.method() == HttpMethod.POST) {
            HttpPostRequestDecoder httpPostRequestDecoder = null;
            try {
                httpPostRequestDecoder = new HttpPostRequestDecoder(httpRequest);
                ObjectNode bodyDataNode = OBJECT_MAPPER.createObjectNode();
                for (InterfaceHttpData interfaceHttpData : httpPostRequestDecoder.getBodyHttpDatas()) {
                    if (interfaceHttpData.getHttpDataType() != InterfaceHttpData.HttpDataType.Attribute) {
                        continue;
                    }
                    Attribute attribute = (Attribute) interfaceHttpData;
                    bodyDataNode.put(attribute.getName(), attribute.getValue());
                }
                requestDataNode.putIfAbsent("body", bodyDataNode);
            } finally {
                if (httpPostRequestDecoder != null) {
                    httpPostRequestDecoder.destroy();
                }
            }
        }

        Object httpController = httpInvocation.getController();
        Method handleMethod = httpInvocation.getMethod();
        Object[] args = ParameterParser.getArgValues(httpInvocation.getParamMetaData(), handleMethod, requestDataNode);
        Object result = handleMethod.invoke(httpController, args);

        if (requestDataNode.get("channel") == null) {
            return;
        }
        FullHttpResponse response;
        if (result != null) {
            byte[] body = OBJECT_MAPPER.writeValueAsBytes(result);
            response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(body));
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, body.length);
        } else {
            response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(Unpooled.EMPTY_BUFFER));
        }
        if (!keepAlive) {
            ctx.writeAndFlush(response).addListeners(ChannelFutureListener.CLOSE);
        } else {
            ctx.writeAndFlush(response);
        }
    }

    private void sendNotFound(ChannelHandlerContext ctx, boolean keepAlive) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND, Unpooled.wrappedBuffer(Unpooled.EMPTY_BUFFER));
        if (!keepAlive) {
            ctx.writeAndFlush(response).addListeners(ChannelFutureListener.CLOSE);
        } else {
            ctx.writeAndFlush(response);
        }
    }
}
