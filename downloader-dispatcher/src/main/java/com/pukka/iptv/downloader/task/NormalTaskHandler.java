package com.pukka.iptv.downloader.task;


import com.pukka.iptv.downloader.config.DispatcherConfig;
import com.pukka.iptv.downloader.log.BizLog;
import com.pukka.iptv.downloader.mq.config.QueueConfig;
import com.pukka.iptv.downloader.mq.model.MsgTask;
import com.pukka.iptv.downloader.mq.model.QueueInfo;
import com.pukka.iptv.downloader.mq.pool.MqUtil;
import com.pukka.iptv.downloader.mq.pool.RabbitMqPool;
import com.pukka.iptv.downloader.nacos.NacosService;
import com.pukka.iptv.downloader.policy.WorkMoreGetMorePolicy;
import com.pukka.iptv.downloader.pool.Node;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ExecutorService;
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
public class NormalTaskHandler extends AbsoluteTaskHandler {

    @Resource(name = "normalSenderThreadPool")
    private ExecutorService executor;
    @Autowired
    private DispatcherConfig config;
    @Autowired
    private WorkMoreGetMorePolicy deliverPolicy;
    @Autowired
    private QueueConfig queueConfig;
    @Autowired
    private NacosService nacosService;
    @Autowired
    private BizLog bizLog;

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

    //获取待执行任务队列的绑定信息和描述
    private QueueInfo getTaskDeclareQueue() {
        return new QueueInfo().exchange(queueConfig.getTaskExchange()).queue(queueConfig.getTaskQueue())
                .routeKey(queueConfig.getTaskRoutingKey()).fetchCount(MAX_TASK_QUEUE_SIZE);
    }

    private QueueInfo getTaskQueue() {
        return new QueueInfo().queue(queueConfig.getTaskQueue()).fetchCount(MAX_TASK_QUEUE_SIZE);
    }

    //声明队列
    private boolean declareQueue() {
        QueueInfo queue = getTaskDeclareQueue();
        if (queue == null) return false;
        //nacos 动态获取 单节点最大同时发送数
        Integer limit = config.getChannelLimit();
        //如果关闭节点，设置最大并发为0
        if (!config.getEnable()) {
            limit = 0;
        }
        //重新设置 管道队列的最大连接数
        RabbitMqPool.RKey key = RabbitMqPool.RKey.newKey(queue, limit);
        RabbitMqPool.me().balanceKeyPoolSize(key);
        //获取
        Node<RabbitMqPool.RKey, Channel> node = RabbitMqPool.me().pickNonBlock(key);
        if (node != null) {
            Channel channel = node.getClient();
            try {
                declare.set(MqUtil.declareOrBindQueueAndExchange(channel, queue));
            } finally {
                RabbitMqPool.me().backClose(node);
            }
        }
        return declare.get();
    }


    private volatile MetaHead metaHead = null;

    @Override
    protected MetaHead getMetaHead() {
        if (metaHead == null) {
            lock.lock();
            try {
                if (!declare.get()) {
                    bizLog.log(i -> log.info("队列还未声明绑定，先进行队列绑定"));

                    if (!declareQueue()) {
                        bizLog.log(i -> log.error("队列声明失败！{}", getTaskDeclareQueue()));
                        return null;
                    }
                    bizLog.log(i -> log.info("队列声明成功"));
                }

                if (metaHead == null)
                    metaHead = new MetaHead()
                            .name("待下载队列任务处理器")
                            .lock(lock)
                            .cacheList(sendList)
                            .queue(getTaskQueue())
                            .executorPool(executor)
                            .dispatcherConfig(config)
                            .queueConfig(queueConfig)
                            .lastSendTime(lastSendTime)
                            .maxWaitTime(MAX_WAIT_TIME)
                            .nacosService(nacosService)
                            .deliverPolicy(deliverPolicy)
                            .maxCacheSize(MAX_TASK_QUEUE_SIZE)
                            .log(bizLog);
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
