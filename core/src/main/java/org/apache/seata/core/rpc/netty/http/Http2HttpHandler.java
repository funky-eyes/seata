package org.apache.seata.core.rpc.netty.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2StreamFrame;
import org.apache.seata.common.rpc.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * The http2 http handler.
 */
public class Http2HttpHandler extends SimpleChannelInboundHandler<Http2StreamFrame> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Http2HttpHandler.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private Http2Headers http2Headers;
    private final StringBuilder bodyBuilder = new StringBuilder();
    private boolean requestHandled = false;
    private boolean headersReceived = false;
    private boolean headersEndStream = false;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http2StreamFrame msg) throws Exception {
        if (requestHandled) {
            return;
        }
        if (msg instanceof Http2HeadersFrame) {
            Http2HeadersFrame headersFrame = (Http2HeadersFrame) msg;
            this.http2Headers = headersFrame.headers();
            headersReceived = true;
            headersEndStream = headersFrame.isEndStream();
            // 如果 header 已经 endStream，且没有 data，则直接处理
            if (headersEndStream) {
                handleRequest(ctx);
            }
        } else if (msg instanceof Http2DataFrame) {
            Http2DataFrame dataFrame = (Http2DataFrame) msg;
            bodyBuilder.append(dataFrame.content().toString(io.netty.util.CharsetUtil.UTF_8));
            if (dataFrame.isEndStream()) {
                handleRequest(ctx);
            }
        }
    }

    private void handleRequest(ChannelHandlerContext ctx) {
        if (requestHandled) {
            return;
        }
        requestHandled = true;
        try {
            if (http2Headers == null) {
                sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST);
                return;
            }
            HttpMethod method = HttpMethod.valueOf(http2Headers.method().toString());
            String path = http2Headers.path().toString();
            String body = bodyBuilder.toString();
            SimpleHttp2Request request = new SimpleHttp2Request(method, path, http2Headers, body);

            // reuse HttpDispatchHandler logic
            boolean keepAlive = true; // In HTTP/2, connections are persistent by default
            QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getPath());
            String requestPath = queryStringDecoder.path();
            HttpInvocation httpInvocation = ControllerManager.getHttpInvocation(requestPath);
            if (httpInvocation == null) {
                sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND);
                return;
            }
            HttpContext<SimpleHttp2Request> httpContext = new HttpContext<>(request, ctx, keepAlive, HttpContext.HTTP_2_0);
            ObjectNode requestDataNode = OBJECT_MAPPER.createObjectNode();
            requestDataNode.putIfAbsent("param", ParameterParser.convertParamMap(queryStringDecoder.parameters()));
            if (request.getMethod() == HttpMethod.POST && request.getBody() != null && !request.getBody().isEmpty()) {
                // assume body is json
                try {
                    ObjectNode bodyDataNode = (ObjectNode) OBJECT_MAPPER.readTree(request.getBody());
                    requestDataNode.putIfAbsent("body", bodyDataNode);
                } catch (Exception e) {
                    LOGGER.warn("Failed to parse http2 body as json: {}", e.getMessage());
                }
            }
            Object httpController = httpInvocation.getController();
            Method handleMethod = httpInvocation.getMethod();
            Object[] args = ParameterParser.getArgValues(httpInvocation.getParamMetaData(), handleMethod, requestDataNode, httpContext);
            Object result = handleMethod.invoke(httpController, args);
            sendResponse(ctx, result);
        } catch (Exception e) {
            LOGGER.error("Exception occurred while processing HTTP2 request: {}", e.getMessage(), e);
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        } finally {
            bodyBuilder.setLength(0);
            http2Headers = null;
            requestHandled = false;
            headersReceived = false;
            headersEndStream = false;
        }
    }

    private void sendResponse(ChannelHandlerContext ctx, Object result) throws Exception {
        byte[] body = result != null ? OBJECT_MAPPER.writeValueAsBytes(result) : new byte[0];
        Http2Headers headers = new DefaultHttp2Headers().status(HttpResponseStatus.OK.codeAsText());
        headers.set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        headers.set(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(body.length));

        ctx.write(new DefaultHttp2HeadersFrame(headers));
        if (body.length > 0) {
            ByteBuf content = Unpooled.wrappedBuffer(body);
            ctx.write(new DefaultHttp2DataFrame(content, true));
        } else {
            ctx.write(new DefaultHttp2DataFrame(Unpooled.EMPTY_BUFFER, true));
        }
        ctx.flush();
    }

    private void sendErrorResponse(ChannelHandlerContext ctx, HttpResponseStatus status) {
        Http2Headers headers = new DefaultHttp2Headers().status(status.codeAsText());
        ctx.writeAndFlush(new DefaultHttp2HeadersFrame(headers, true));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // This is a common exception when the client (like curl) closes the connection after receiving the response.
        // We can safely ignore it by simply closing the context.
        if (cause instanceof java.io.IOException) {
            LOGGER.trace("Client connection closed: {}", cause.getMessage());
        } else {
            LOGGER.error("Exception caught in Http2HttpHandler: ", cause);
        }
        ctx.close();
    }
}
