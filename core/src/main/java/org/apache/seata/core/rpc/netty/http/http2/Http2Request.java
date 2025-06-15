package org.apache.seata.core.rpc.netty.http.http2;

import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2HeadersFrame;

public class Http2Request {


    Http2HeadersFrame http2HeadersFrame;

    Http2DataFrame http2DataFrame;

    public Http2Request(Http2HeadersFrame http2HeadersFrame, Http2DataFrame http2DataFrame) {
        this.http2HeadersFrame = http2HeadersFrame;
        this.http2DataFrame = http2DataFrame;
    }

    public Http2HeadersFrame getHttp2HeadersFrame() {
        return http2HeadersFrame;
    }

    public void setHttp2HeadersFrame(Http2HeadersFrame http2HeadersFrame) {
        this.http2HeadersFrame = http2HeadersFrame;
    }

    public Http2DataFrame getHttp2DataFrame() {
        return http2DataFrame;
    }

    public void setHttp2DataFrame(Http2DataFrame http2DataFrame) {
        this.http2DataFrame = http2DataFrame;
    }
}
