package com.pukka.iptv.downloader.dispatcher;


import cn.hutool.core.thread.ThreadFactoryBuilder;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.pukka.iptv.downloader.config.DispatcherConfig;
import com.pukka.iptv.downloader.log.BizLog;
import com.pukka.iptv.downloader.nacos.listener.NacosNotify;
import com.pukka.iptv.downloader.task.TaskHandler;
import com.pukka.iptv.downloader.threadpool.ThreadUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import sun.misc.ThreadGroupUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


/**
 * @Author: wz
 * @Date: 2021/10/15 11:41
 * @Description: 调度中心  负责提供暴漏调度接口和内部定时，负责收集和触发组件内所有的调度处理器
 */
@Slf4j
@Component
public class DispatcherCenter implements NacosNotify {
    @Autowired
    private BizLog l;
    @Resource(name = "dispatcherScheduleThreadPool")
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
    @Autowired
    private DispatcherConfig config;
    //定时任务线程池future
    private volatile ScheduledFuture<?> scheduledFuture = null;


    @PostConstruct
    private void init() {
        //启动时就判断是否启动内部定时任务
        if (config.getEnableSchedule()) {
            doSchedule();
        }
    }

    @Override//接收nacos配置刷新的事件
    public void configRefreshEvent() {
        if (config.getEnableSchedule()) {
            log.info("开启 调度器的内置定时任务");
            doSchedule();
        } else {
            log.info("关闭 调度器的内置定时任务");
            ThreadUtils.cancelSchedule(scheduledFuture);
        }
    }

    //执行内部定时任务
    private void doSchedule() {
        // 内部定时任务执行 每秒执行一次
        if (ThreadUtils.isNullOrDone(scheduledFuture)) {
            scheduledFuture =
                    scheduledThreadPoolExecutor.scheduleAtFixedRate(() -> {
                        dispatch();
                    }, 1, config.getIntervalTime(), TimeUnit.MILLISECONDS);
        }
    }

    //执行调度
    public void dispatch() {
        if (config.getEnable()) {
            doWork();
        }
    }


    private void doWork() {
        ApplicationContext app = SpringUtil.getApplicationContext();
        if (app != null) {
            l.log(l -> log.info("下载模块 调度中心开始执行调度"));
            //找出容器中所有的处理器
            Map<String, TaskHandler> taskHandler = app.getBeansOfType(TaskHandler.class);
            for (Map.Entry<String, TaskHandler> item : taskHandler.entrySet()) {
                try {
                    item.getValue().handler();
                    //executor.execute(() -> item.getValue().handler());
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }
        }
    }


    public static void main(String[] args) {

    }

    private void demo1() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(10);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        ThreadFactory threadFactory = ThreadFactoryBuilder.create()
                .setDaemon(false).setNamePrefix("##biz-test##").build();
        executor.setThreadFactory(threadFactory);
        executor.initialize();

        ReentrantLock lock = new ReentrantLock();
        ReentrantLock innerLock = new ReentrantLock();
        int x = 20;
        CountDownLatch latch = new CountDownLatch(x);
        AtomicInteger data = new AtomicInteger(0);
        lock.lock();
        for (int i = 0; i < x; i++) {
            executor.submit(() -> {
                try {
                    Thread thread = Thread.currentThread();
                    log.info("thread name :{},thread id:{},thread group:{}", thread.getName(), thread.getId(), thread.getThreadGroup());
                    thread.sleep(RandomUtil.randomLong(2000, 5000));
                    innerLock.lock();

                    int result = data.incrementAndGet();
                    log.info("get data:{}", result);
                    innerLock.unlock();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        log.info("main thread wait");
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        lock.unlock();
        log.info("all done! data={}", data);
        log.info("bbq 666");

    }

}
