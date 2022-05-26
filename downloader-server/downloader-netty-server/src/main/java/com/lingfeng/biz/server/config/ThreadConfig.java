package com.lingfeng.biz.server.config;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @Author: wz
 * @Date: 2022/5/18 15:13
 * @Description:
 */
@Slf4j
@Configuration
public class ThreadConfig {
    List<ExecutorService> executorServices = new ArrayList<>();
    List<ThreadPoolTaskExecutor> taskExecutors = new ArrayList<>();

    @PreDestroy
    private void destroy() {
        closeThreadPool();
    }

    public void closeThreadPool() {
        log.info("系统关闭，关闭连接池");
        for (ExecutorService executor : executorServices) {
            executor.shutdown();
        }
        for (ThreadPoolTaskExecutor taskExecutor : taskExecutors) {
            taskExecutor.shutdown();
        }
    }

    //调度器发送线程池
    @Bean("dispatcherSenderThreadPool")
    public ThreadPoolTaskExecutor dispatcherSenderThreadPool() {
        ThreadFactory factory = ThreadFactoryBuilder.create()
                .setNamePrefix("##dispatcherSenderThreadPool##").setDaemon(false)
                .build();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setKeepAliveSeconds(60);
        executor.setThreadFactory(factory);
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setAllowCoreThreadTimeOut(true);
        taskExecutors.add(executor);
        executor.initialize();
        return executor;
    }
}
