package org.apache.seata.core.rpc.netty.grpc;

import com.google.protobuf.Any;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2Headers;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.protocol.generated.GrpcMessageProto;
import org.apache.seata.core.serializer.Serializer;
import org.apache.seata.core.serializer.SerializerServiceLoader;
import org.apache.seata.core.serializer.SerializerType;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class GrpcEncoder extends ChannelOutboundHandlerAdapter {
    private final AtomicBoolean headerSent = new AtomicBoolean(false);

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!(msg instanceof RpcMessage)){
            throw new UnsupportedOperationException("GrpcEncoder not support class:" + msg.getClass());
        }

        RpcMessage rpcMessage = (RpcMessage) msg;
        byte messageType = rpcMessage.getMessageType();
        Map<String, String> headMap = rpcMessage.getHeadMap();
        Object body = rpcMessage.getBody();
        int id = rpcMessage.getId();

        if (headerSent.compareAndSet(false, true))
        {
            Http2Headers headers = new DefaultHttp2Headers();
            headers.add(GrpcHeaderEnum.HTTP2_STATUS.header, String.valueOf(200));
            headers.add(GrpcHeaderEnum.GRPC_STATUS.header, String.valueOf(0));
            headers.add(GrpcHeaderEnum.GRPC_CONTENT_TYPE.header, "application/grpc");
            ctx.writeAndFlush(new DefaultHttp2HeadersFrame(headers));
        }

        Serializer  serializer = SerializerServiceLoader.load(SerializerType.getByCode(SerializerType.GRPC.getCode()));
        Any messageBody = Any.parseFrom(serializer.serialize(body));
        GrpcMessageProto grpcMessageProto = GrpcMessageProto.newBuilder()
                .setBody(messageBody)
                .putAllHeadMap(headMap)
                .setMessageType(messageType)
                .setId(id).build();
        byte[] bodyBytes = grpcMessageProto.toByteArray();
        if (bodyBytes != null)
        {
            byte[] messageWithPrefix = new byte[bodyBytes.length + 5];
            // 第一个字节为0，表示不压缩
            messageWithPrefix[0] = 0;
            ByteBuffer buffer = ByteBuffer.allocate(4);
            buffer.putInt(bodyBytes.length);
            byte[] lengthBytes = buffer.array();
            // 后四个字节表示长度
            System.arraycopy(lengthBytes, 0, messageWithPrefix, 1, 4);
            // 剩余字节是body
            System.arraycopy(bodyBytes, 0, messageWithPrefix, 5, bodyBytes.length);
            ctx.writeAndFlush(new DefaultHttp2DataFrame(Unpooled.wrappedBuffer(messageWithPrefix)));
        }
    }

}
