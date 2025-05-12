package org.apache.seata.spring.boot.autoconfigure.http;

import org.apache.seata.core.rpc.netty.http.ControllerManager;
import org.apache.seata.core.rpc.netty.http.HttpInvocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.bind.annotation.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

public class RestControllerBeanPostProcessorTest {

    @Mock
    private ControllerManager controllerManager;

    private RestControllerBeanPostProcessor processor;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        processor = new RestControllerBeanPostProcessor();
    }

    @Test
    public void testPostProcessAfterInitialization() throws Exception {
        // Mock the bean and its annotations
        TestController mockBean = new TestController();

        // Call the method under test
        processor.postProcessAfterInitialization(mockBean, "testController");

        // Verify that the paths were added correctly
        HttpInvocation httpInvocation = new HttpInvocation();
        httpInvocation.setPath("/path");
        verify(controllerManager).addHttpInvocation(httpInvocation);
    }

    @RestController
    @RequestMapping("/base")
    static class TestController {

        @GetMapping("/get")
        public String getMethod(@RequestParam String param) {
            return "GET";
        }

        @PostMapping("/post")
        public String postMethod(@RequestBody String body) {
            return "POST";
        }
    }
}



