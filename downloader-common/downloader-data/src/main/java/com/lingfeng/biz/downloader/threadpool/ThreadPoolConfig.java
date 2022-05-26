package com.lingfeng.biz.downloader.threadpool;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * @Author: wz
 * @Date: 2021/10/13 17:29
 * @Description:
 */
@Slf4j
@Configuration
public class ThreadPoolConfig {

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

    @Bean("dispatcherScheduleThreadPool")// 调度器定时任务触发线程池
    public ScheduledThreadPoolExecutor dispatcherScheduleThreadPool() {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
        executor.setMaximumPoolSize(2);
        executor.setRemoveOnCancelPolicy(true);
        executor.setKeepAliveTime(5, TimeUnit.SECONDS);
        ThreadFactory factory = ThreadFactoryBuilder.create()
                .setDaemon(false).setNamePrefix("##dispatcherScheduleThreadPool##").build();
        executor.setThreadFactory(factory);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executorServices.add(executor);
        return executor;
    }

    @Bean//下载器 定时任务触发线程池
    public ScheduledThreadPoolExecutor downloaderScheduleThreadPool() {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(2);
        executor.setMaximumPoolSize(2);
        executor.setRemoveOnCancelPolicy(true);
        executor.setKeepAliveTime(5, TimeUnit.SECONDS);
        ThreadFactory factory = ThreadFactoryBuilder.create()
                .setDaemon(false).setNamePrefix("##downloaderScheduleThreadPool##").build();
        executor.setThreadFactory(factory);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executorServices.add(executor);
        return executor;
    }


    @Bean//下载器 定时任务触发线程池
    public ScheduledThreadPoolExecutor testDelay() {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
        executor.setMaximumPoolSize(2);
        executor.setRemoveOnCancelPolicy(true);
        executor.setKeepAliveTime(5, TimeUnit.SECONDS);
        ThreadFactory factory = ThreadFactoryBuilder.create()
                .setDaemon(false).setNamePrefix("##testDelay##").build();
        executor.setThreadFactory(factory);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executorServices.add(executor);
        return executor;
    }


    @Bean//执行下载任务的线程池
    public ThreadPoolTaskExecutor downloaderThreadPool() {
        ThreadFactory factory = ThreadFactoryBuilder.create()
                .setNamePrefix("##downloader##").setDaemon(false)
                .build();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setKeepAliveSeconds(30);
        executor.setThreadFactory(factory);
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(64);
        executor.setQueueCapacity(0);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setAllowCoreThreadTimeOut(true);
        executor.initialize();
        taskExecutors.add(executor);
        return executor;
    }
}
