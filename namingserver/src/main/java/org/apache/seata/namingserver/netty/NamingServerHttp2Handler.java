package org.apache.seata.namingserver.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import org.apache.seata.core.rpc.netty.grpc.GrpcHeaderEnum;
import org.apache.seata.namingserver.listener.Watcher;
import org.apache.seata.namingserver.manager.ClusterWatcherManager;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class NamingServerHttp2Handler extends ChannelDuplexHandler {

    private final AtomicBoolean headerSent = new AtomicBoolean(false);

    private ClusterWatcherManager clusterWatcherManager;

    public NamingServerHttp2Handler(ClusterWatcherManager clusterWatcherManager) {
        this.clusterWatcherManager = clusterWatcherManager;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof Http2HeadersFrame) {
            if (headerSent.compareAndSet(false, true)) {
                Http2Headers headers = new DefaultHttp2Headers();
                headers.add(GrpcHeaderEnum.HTTP2_STATUS.header, String.valueOf(200));
                ctx.writeAndFlush(new DefaultHttp2HeadersFrame(headers, false));
            }

            Http2HeadersFrame http2HeadersFrame = (Http2HeadersFrame) msg;
            CharSequence path = http2HeadersFrame.headers().path();
            QueryStringDecoder queryStringDecoder = new QueryStringDecoder(path.toString());
            Map<String, List<String>> parameters = queryStringDecoder.parameters();
            String vGroup = parameters.get("vGroup").get(0);
            String clientTerm = parameters.get("clientTerm").get(0);
            InetSocketAddress inetSocketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
            Watcher<Channel> watcher = new Watcher<>(vGroup, ctx.channel(), 0, Long.parseLong(clientTerm), inetSocketAddress.getAddress().getHostAddress());
            // never time out
            watcher.setTimeout(-1);
            clusterWatcherManager.registryWatcher(watcher);
        }
    }
}
