package com.pukka.iptv.downloader.task;

import com.pukka.iptv.downloader.config.DispatcherConfig;
import com.pukka.iptv.downloader.mq.config.QueueConfig;
import com.pukka.iptv.downloader.mq.model.MsgTask;
import com.pukka.iptv.downloader.mq.model.QueueInfo;
import com.pukka.iptv.downloader.nacos.NacosService;
import com.pukka.iptv.downloader.policy.TrackQueuePolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;


/**
 * @Author: wz
 * @Date: 2021/10/15 11:41
 * @Description: 回调通知失败的处理队列
 */
@Slf4j
//@Component
public class CallbackFailedTaskHandler extends AbsoluteTaskHandler {
    // @Resource(name = "downloaderDispatcherThreadPool")
    //private ThreadPoolTaskExecutor executor;
    @Autowired
    private QueueConfig queueConfig;
    @Autowired
    private DispatcherConfig config;
    @Autowired
    private TrackQueuePolicy trackQueuePolicy;
    @Autowired
    private NacosService nacosService;


    //上次发送时间
    private final static AtomicLong lastSendTime = new AtomicLong();
    //最大等待时间
    private final static int MAX_WAIT_TIME = 5 * 1000;//5s
    //缓冲队列最大长度
    private final static int MAX_TASK_QUEUE_SIZE = 50;
    //缓冲任务队列 问题:缓冲队列可能存在任务丢失的情况 需要验证
    private volatile List<MsgTask> sendList = new ArrayList<>(10);
    //执行锁
    private volatile ReentrantLock lock = new ReentrantLock();

    //获取待执行任务队列
    private QueueInfo getRetryTaskQueue() {
        return new QueueInfo().exchange("").queue("")
                .routeKey("").fetchCount(MAX_TASK_QUEUE_SIZE);
    }

    private volatile MetaHead metaHead = null;

    @Override
    protected MetaHead getMetaHead() {
        if (true) return null;
        if (metaHead == null) {
            lock.lock();
            try {
                if (metaHead == null)
                    metaHead = new MetaHead()
                            .name("重试队列任务处理器")
                            .lock(lock)
                            .cacheList(sendList)
                            //.executorPool(executor)
                            .dispatcherConfig(config)
                            .queueConfig(queueConfig)
                            .nacosService(nacosService)
                            .queue(getRetryTaskQueue())
                            .deliverPolicy(trackQueuePolicy)
                            .lastSendTime(lastSendTime)
                            .maxWaitTime(MAX_WAIT_TIME)
                            .maxCacheSize(MAX_TASK_QUEUE_SIZE);
            } finally {
                lock.unlock();
            }
        }
        return metaHead;
    }

    @Override
    public boolean handler() {
        // return super.handler();
        return false;
    }
}
