package org.apache.seata.server.controller;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

/**
 * @author jianbin@apache.org
 */
@RestController
@RequestMapping("/test/")
public class ClientRegistryController {

	private static final ThreadPoolExecutor executor =  new ThreadPoolExecutor(1, 1,
		0L, TimeUnit.MILLISECONDS,
		new LinkedBlockingQueue<Runnable>());

	@PostMapping("/registry")
	public ResponseBodyEmitter addListener() {
		ResponseBodyEmitter responseBodyEmitter = new ResponseBodyEmitter(-1L);
		executor.submit(() -> {
			for (int i = 0; i < 10; i++) {
				try {
					Thread.sleep(1000);
					responseBodyEmitter.send(System.currentTimeMillis());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
			responseBodyEmitter.complete();
		});
		return responseBodyEmitter;
	}


}
