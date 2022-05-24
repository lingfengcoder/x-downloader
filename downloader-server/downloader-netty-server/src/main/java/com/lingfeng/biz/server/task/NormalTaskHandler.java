package com.lingfeng.biz.server.task;


import com.lingfeng.biz.downloader.log.BizLog;
import com.lingfeng.biz.downloader.model.DownloadTask;
import com.lingfeng.biz.downloader.model.MsgTask;
import com.lingfeng.biz.server.DownloaderServer;
import com.lingfeng.biz.server.cache.WaterCacheQueue;
import com.lingfeng.biz.server.config.DispatcherConfig;
import com.lingfeng.biz.server.policy.WorkMoreGetMorePlusPolicy;
import com.lingfeng.dutation.store.DbStore;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author: wz
 * @Date: 2021/10/15 11:48
 * @Description: 待下载任务处理类
 */
@Slf4j
@Component
@Getter
public class NormalTaskHandler extends AbsoluteTaskHandler {

    @Resource(name = "dispatcherSenderThreadPool")
    private ThreadPoolTaskExecutor executor;
    @Autowired
    private DispatcherConfig config;
    //更换基于数学模型的多劳多得算法
    @Autowired
    private WorkMoreGetMorePlusPolicy deliverPolicy;
    @Autowired
    private DownloaderServer downloaderServer;
    @Autowired
    private BizLog bizLog;
    @Autowired
    private DbStore store;

    //上次发送时间(用于计算超时)
    private final static AtomicLong lastSendTime = new AtomicLong();
    //缓冲队列执行器最大等待时间
    private final static int MAX_WAIT_TIME = 5 * 1000;//5s
    //缓冲队列最大长度
    private final static int MAX_TASK_QUEUE_SIZE = 50;
    //缓冲任务队列 问题:缓冲队列可能存在任务丢失的情况 需要验证
    private volatile List<MsgTask> sendList = new ArrayList<>(10);
    //执行锁
    private static final ReentrantLock lock = new ReentrantLock();
    //队列是否声明成功
    private volatile AtomicBoolean declare = new AtomicBoolean(false);
    private volatile MetaHead metaHead = null;
    private volatile WaterCacheQueue<DownloadTask> cacheQueue;

    @Override
    protected MetaHead getMetaHead() {

        if (metaHead == null) {
            lock.lock();
            try {
                if (!declare.get()) {
                    bizLog.log(i -> log.info("队列还未声明绑定，先进行队列绑定"));

                    bizLog.log(i -> log.info("队列声明成功"));
                }

                if (metaHead == null) {
                    cacheQueue = new WaterCacheQueue<>(5, 10, new PriorityBlockingQueue<>());
                    metaHead = new MetaHead()
                            .name("待下载队列任务处理器")
                            .lock(lock)
                            .cacheQueue(cacheQueue)
                            //.queue(getTaskQueue())
                            .executorPool(executor)
                            .dispatcherConfig(config)
                            // .queueConfig(queueConfig)
                            .lastSendTime(lastSendTime)
                            .maxWaitTime(MAX_WAIT_TIME)
                            // .nacosService(nacosService)
                            .deliverPolicy(deliverPolicy)
                            .maxCacheSize(MAX_TASK_QUEUE_SIZE)
                            .log(bizLog);
                }
            } finally {
                lock.unlock();
            }
        }
        return metaHead;
    }


    @Override
    public boolean handler() {
        return super.handler();
    }


}
