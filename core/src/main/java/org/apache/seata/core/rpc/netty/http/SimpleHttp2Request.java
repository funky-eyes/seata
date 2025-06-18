package org.apache.seata.core.rpc.netty.http;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http2.Http2Headers;

public class SimpleHttp2Request {
    private final HttpMethod method;
    private final String path;
    private final Http2Headers headers;
    private final String body;

    public SimpleHttp2Request(HttpMethod method, String path, Http2Headers headers, String body) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.body = body;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public Http2Headers getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }
}

