package com.lingfeng.biz.server.handler.taskhandler;


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
 * @Description: 待下载任务处理器构建类
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

    private final static int lowWaterLevel = 5;
    private final static int highWaterLevel = 10;
    //执行锁
    private static final ReentrantLock lock = new ReentrantLock();
    private volatile HandlerHead handlerHead = null;
    private volatile WaterCacheQueue<DownloadTask> cacheQueue;

    @Override
    protected HandlerHead getHandlerHead() {
        if (handlerHead == null) {
            lock.lock();
            try {

                if (handlerHead == null) {
                    cacheQueue = new WaterCacheQueue<>(lowWaterLevel, highWaterLevel, new PriorityBlockingQueue<>());
                    handlerHead = new HandlerHead()
                            .name("待下载队列任务处理器")
                            .lock(lock)
                            .cacheQueue(cacheQueue)
                            .executorPool(executor)
                            .dispatcherConfig(config)
                            .lastSendTime(lastSendTime)
                            .maxWaitTime(MAX_WAIT_TIME)
                            .routePolicy(deliverPolicy)
                            .log(bizLog);
                }
            } finally {
                lock.unlock();
            }
        }
        return handlerHead;
    }


    @Override
    public boolean handler() {
        return super.handler();
    }


}
