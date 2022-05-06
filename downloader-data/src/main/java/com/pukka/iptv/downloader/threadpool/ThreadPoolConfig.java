package com.pukka.iptv.downloader.threadpool;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Date;
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

    @Bean//下载器 定时任务触发线程池
    public ScheduledThreadPoolExecutor downloaderScheduleThreadPool() {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
        executor.setMaximumPoolSize(3);
        ThreadFactory factory = ThreadFactoryBuilder.create()
                .setDaemon(false).setNamePrefix("##downloaderScheduleThreadPool##").build();
        executor.setThreadFactory(factory);
        //executor.setRejectedExecutionHandler();
        executorServices.add(executor);
        return executor;
    }

    @Bean//下载器 定时任务触发线程池
    public ScheduledThreadPoolExecutor callbackScheduleThreadPool() {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
        executor.setMaximumPoolSize(3);
        ThreadFactory factory = ThreadFactoryBuilder.create()
                .setDaemon(false).setNamePrefix("##callbackScheduleThreadPool##").build();
        executor.setThreadFactory(factory);
        //executor.setRejectedExecutionHandler();
        executorServices.add(executor);
        return executor;
    }


    @Bean//下载器 定时任务触发线程池
    public ScheduledThreadPoolExecutor testDelay() {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
        executor.setMaximumPoolSize(3);
        ThreadFactory factory = ThreadFactoryBuilder.create()
                .setDaemon(false).setNamePrefix("##testDelay##").build();
        executor.setThreadFactory(factory);
        //executor.setRejectedExecutionHandler();
        executorServices.add(executor);
        return executor;
    }

    @Bean//调度器 定时触发线程池
    public ScheduledThreadPoolExecutor dispatcherScheduleThreadPool() {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
        executor.setMaximumPoolSize(2);
        executor.setRemoveOnCancelPolicy(true);
        ThreadFactory factory = ThreadFactoryBuilder.create()
                .setDaemon(false).setNamePrefix("##dispatcherScheduleThreadPool##").build();
        executor.setThreadFactory(factory);
        executorServices.add(executor);
        return executor;
    }

    @Bean
    public ExecutorService retrySenderThreadPool() {
        ThreadFactory factory = ThreadFactoryBuilder.create()
                .setNamePrefix("##retrySenderThreadPool##").setDaemon(false)
                .build();
        ExecutorService executor = Executors.newCachedThreadPool(factory);
        executorServices.add(executor);
        return executor;
    }

    @Bean
    public ExecutorService normalSenderThreadPool() {
        ThreadFactory factory = ThreadFactoryBuilder.create()
                .setNamePrefix("##normalSenderThreadPool##").setDaemon(false)
                .build();
        ExecutorService executor = Executors.newCachedThreadPool(factory);
        executorServices.add(executor);
        return executor;
    }

    @Bean//执行下载任务的线程池
    public ExecutorService downloaderThreadPool() {
        ThreadFactory factory = ThreadFactoryBuilder.create()
                .setNamePrefix("##downloader##").setDaemon(false)
                .build();
        ExecutorService executor = Executors.newCachedThreadPool(factory);
        executorServices.add(executor);
        return executor;
    }

    private static void testInterrupt() throws InterruptedException {
        LinkedBlockingQueue<Integer> queue = new LinkedBlockingQueue<>();

        queue.offer(1);
        queue.offer(2);
        queue.offer(3);
        Thread thread = new Thread(() -> {
            try {
                while (queue.size() > -1) {
                    Integer take = queue.take();
                    log.info("得到{}", take);
                    Thread.currentThread().sleep(500);
                }
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
            }
        }, "bbq");
        thread.start();
        Thread.currentThread().sleep(5000);
        thread.interrupt();
    }

    public static void main1(String[] args) throws InterruptedException {
        testInterrupt();
        Thread.currentThread().join();
    }

    public static void main(String[] args) throws InterruptedException {
        ThreadPoolExecutor executor = buildThreadPool();
        int x = 30;
        for (int i = 0; i < x; i++) {
            executor.execute(() -> {
                Thread thread = Thread.currentThread();
                log.info("{}开始执行", thread.getName());
                long begin = System.currentTimeMillis();
                int num = 8;
                boolean flag = true;
                while (num > 0) {
                    try {
                        --num;
                        TimeUnit.SECONDS.sleep(1);
                        boolean interrupted = thread.isInterrupted();
                        if (interrupted) {
                            log.info("{}有中断信号", thread.getName());
                        }
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                        throw new RuntimeException();
                    }
                }
                log.info("{}执行完毕 耗时:{}", thread.getName(), (System.currentTimeMillis() - begin));
            });
        }

        TimeUnit.MICROSECONDS.sleep(8000);

        executor.setCorePoolSize(1);

        //Thread.currentThread().join();
    }

    public static ThreadPoolExecutor buildThreadPool() {

        BlockingQueue queue = new LinkedBlockingQueue<>(10);
        ThreadPoolExecutor executor =
                new ThreadPoolExecutor(2, 3,
                        0, TimeUnit.SECONDS, queue);


        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());


        ThreadFactory factory = ThreadFactoryBuilder.create()
                .setNamePrefix("##biz_dynamic_adjust_pool##").setDaemon(false)
                .build();
        executor.setThreadFactory(factory);
        return executor;
    }
}
