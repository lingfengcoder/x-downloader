package com.pukka.iptv.downloader.task.multiplyhandler;


import cn.hutool.core.thread.ThreadFactoryBuilder;
import com.pukka.iptv.downloader.config.NodeConfig;
import com.pukka.iptv.downloader.model.DownloadTask;
import com.pukka.iptv.downloader.task.downloader.AbstractDownloader;
import com.pukka.iptv.downloader.task.downloader.Downloader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * @Author: wz
 * @Date: 2022/1/4 18:40
 * @Description: 多任务处理器
 */
@Slf4j
public class DemoMultiplyTaskHandler extends AbstractMultiplyTaskPool<DownloadTask> {
    //此处由于多个下载器是共享的 多任务处理器 所以在没有给各自分配 队列限制前，队列和锁也必须共享
    private final static ReentrantLock lock = new ReentrantLock();
    private final static ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    private final static AtomicInteger limit = new AtomicInteger(1);

    static {
        ThreadFactory factory = ThreadFactoryBuilder.create()
                .setNamePrefix("##downloader##").setDaemon(false)
                .build();
        executor.setKeepAliveSeconds(30);
        executor.setThreadFactory(factory);
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(64);
        executor.setQueueCapacity(0);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setAllowCoreThreadTimeOut(true);
        executor.initialize();
    }

    @Override
    protected ThreadPoolTaskExecutor getExecutor() {
        return executor;
    }

    @Override
    protected Lock getLock() {
        return lock;
    }


    @Override
    protected boolean doWork(DownloadTask task) {
        log.info("taskId={} 开始执行", task.getTaskId());
        try {
            TimeUnit.SECONDS.sleep(4);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
        log.info("taskId={} 执行完毕", task.getTaskId());
        return true;
    }

    @Override
    public int getLimit() {
        return limit.get();
    }

    @Override
    public void setLimit(int x) {
        limit.set(x);
    }
}



