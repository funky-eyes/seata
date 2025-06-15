package org.apache.seata.core.rpc.netty.http.http2;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import org.apache.seata.core.rpc.netty.http.grpc.GrpcDecoder;
import org.apache.seata.core.rpc.netty.http.grpc.GrpcEncoder;
import org.apache.seata.core.rpc.netty.http.grpc.GrpcHeaderEnum;

public class Http2DetectHandler extends ChannelDuplexHandler {

    private final ChannelHandler[] serverHandlers;

    public Http2DetectHandler(ChannelHandler[] serverHandlers) {
        this.serverHandlers = serverHandlers;
    }

    @Override public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Http2HeadersFrame) {
            Http2HeadersFrame headersFrame = (Http2HeadersFrame) msg;
            CharSequence contentType = headersFrame.headers().get(HttpHeaderNames.CONTENT_TYPE);
            if (contentType != null && GrpcHeaderEnum.GRPC_CONTENT_TYPE.header.equalsIgnoreCase(contentType.toString())) {
                ctx.pipeline().addLast(new GrpcDecoder());
                ctx.pipeline().addLast(new GrpcEncoder());
                ctx.pipeline().addLast(serverHandlers);
            } else {
                ctx.pipeline().addLast(new Http2DispatchHandler());
            }
            ctx.pipeline().remove(this);
            ctx.fireChannelRead(msg);
        }
    }
}
