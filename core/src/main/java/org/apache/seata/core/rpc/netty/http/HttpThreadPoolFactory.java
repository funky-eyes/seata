package org.apache.seata.core.rpc.netty.http;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.seata.common.thread.NamedThreadFactory;
import org.apache.seata.core.rpc.netty.NettyServerConfig;

public class HttpThreadPoolFactory {

   private static final Map<String, ExecutorService> threadPools = new ConcurrentHashMap<>();

    static  {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override public void run() {
                threadPools.values().forEach(ExecutorService::shutdown);
            }
        }));
    }

   public static ExecutorService getHttpHandlerThreads(){
        return threadPools.computeIfAbsent("http",value -> new ThreadPoolExecutor(
                NettyServerConfig.getMinHttpPoolSize(),
                NettyServerConfig.getMaxHttpPoolSize(),
                NettyServerConfig.getHttpKeepAliveTime(),
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(NettyServerConfig.getMaxHttpTaskQueueSize()),
                new NamedThreadFactory("HTTPHandlerThread", NettyServerConfig.getMaxHttpPoolSize()),
                new ThreadPoolExecutor.AbortPolicy()
        ));
    }

}
