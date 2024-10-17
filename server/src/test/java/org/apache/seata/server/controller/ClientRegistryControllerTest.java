package org.apache.seata.server.controller;

import java.io.IOException;
import java.net.URI;
import java.nio.CharBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.hc.client5.http.async.methods.AbstractCharResponseConsumer;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.Method;
import org.apache.hc.core5.http.nio.support.BasicRequestProducer;
import org.apache.hc.core5.http.protocol.BasicHttpContext;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * @author jianbin@apache.org
 */
@SpringBootTest
@AutoConfigureMockMvc
public class ClientRegistryControllerTest {

	@Autowired
	private ClientRegistryController clientRegistryController;

	@Autowired
	private MockMvc mockMvc;

	@BeforeAll
	public static void setUp(ApplicationContext context) throws InterruptedException {
	}


	@Test
	void registry() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.post("/test/registry")) //设置请求地址
			.andExpect(MockMvcResultMatchers.status().isOk()) //断言返回状态码为200
			.andDo(MockMvcResultHandlers.print()); //在控制台打印日志
	}
	
   // @Test
    void testMulitResponse() throws Exception {
        PoolingAsyncClientConnectionManager poolingAsyncClientConnectionManager =
            new PoolingAsyncClientConnectionManager();
        poolingAsyncClientConnectionManager.setMaxTotal(10);
        poolingAsyncClientConnectionManager.setDefaultMaxPerRoute(10);
        CloseableHttpAsyncClient client =
            HttpAsyncClients.custom().setConnectionManager(poolingAsyncClientConnectionManager)
                .setDefaultRequestConfig(
                    RequestConfig.custom().setConnectionRequestTimeout(Timeout.of(30 * 60, TimeUnit.SECONDS))
                        .setConnectTimeout(Timeout.of(30 * 60, TimeUnit.SECONDS)).build())
                .build();
        client.start();
        URIBuilder builder = new URIBuilder("http://127.0.0.1:7091/test/registry");
        URI uri = builder.build();
        CountDownLatch countDownLatch = new CountDownLatch(5);
        client.execute(new BasicRequestProducer(Method.POST, uri), new AbstractCharResponseConsumer<HttpResponse>() {
            HttpResponse response;

            @Override
            public void releaseResources() {}

            @Override
            protected int capacityIncrement() {
                return Integer.MAX_VALUE;
            }

            @Override
            protected void data(CharBuffer src, boolean endOfStream) {
                StringBuilder responseBuilder = new StringBuilder();
                while (src.hasRemaining()) {
                    responseBuilder.append(src.get());
                }
                System.out.println(System.currentTimeMillis() + " , Response: " + responseBuilder);
                countDownLatch.countDown();
            }

            @Override
            protected void start(HttpResponse response, ContentType contentType) throws HttpException, IOException {
                this.response = response;
            }

            @Override
            protected HttpResponse buildResult() throws IOException {
                return response;
            }
        }, new BasicHttpContext(), new FutureCallback<HttpResponse>() {
            @Override
            public void completed(HttpResponse result) {
                System.out.println("result code:" + result.getCode());
            }

            @Override
            public void failed(Exception ex) {

            }

            @Override
            public void cancelled() {

            }
        });
        countDownLatch.await(11, TimeUnit.SECONDS);
        client.close();
    }
}
