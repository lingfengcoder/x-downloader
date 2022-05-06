package com.pukka.iptv.downloader.task;

import com.pukka.iptv.downloader.config.DispatcherConfig;
import com.pukka.iptv.downloader.log.BizLog;
import com.pukka.iptv.downloader.mq.config.QueueConfig;
import com.pukka.iptv.downloader.mq.model.MsgTask;
import com.pukka.iptv.downloader.mq.model.QueueInfo;
import com.pukka.iptv.downloader.mq.pool.MqUtil;
import com.pukka.iptv.downloader.mq.pool.RabbitMqPool;
import com.pukka.iptv.downloader.nacos.NacosService;
import com.pukka.iptv.downloader.policy.TrackQueuePolicy;
import com.pukka.iptv.downloader.pool.Node;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;


/**
 * @Author: wz
 * @Date: 2021/10/15 11:41
 * @Description: 重试队列处理器
 */
@Slf4j
@Component
public class RetryTaskHandler extends AbsoluteTaskHandler {
    @Resource(name = "dispatcherSenderThreadPool")
    private ThreadPoolTaskExecutor executor;
    @Autowired
    private QueueConfig queueConfig;
    @Autowired
    private DispatcherConfig config;
    @Autowired
    private TrackQueuePolicy trackQueuePolicy;
    @Autowired
    private NacosService nacosService;
    @Autowired
    private BizLog bizLog;

    //上次发送时间
    private final static AtomicLong lastSendTime = new AtomicLong();
    //最大等待时间
    private final static int MAX_WAIT_TIME = 5 * 1000;//5s
    //缓冲队列最大长度
    private final static int MAX_TASK_QUEUE_SIZE = 50;
    //缓冲任务队列 问题:缓冲队列可能存在任务丢失的情况 需要验证
    private final List<MsgTask> sendList = new ArrayList<>(10);
    //执行锁
    private static final ReentrantLock lock = new ReentrantLock();
    //队列是否声明成功
    private final AtomicBoolean declare = new AtomicBoolean(false);

    //获取待执行任务队列
    private QueueInfo getRetryTaskDeclareQueue() {
        return new QueueInfo().exchange(queueConfig.getRetryExchange()).queue(queueConfig.getRetryQueue())
                .routeKey(queueConfig.getRetryRoutingKey()).fetchCount(MAX_TASK_QUEUE_SIZE);
    }

    private QueueInfo getRetryTaskQueue() {
        return new QueueInfo().queue(queueConfig.getRetryQueue()).fetchCount(MAX_TASK_QUEUE_SIZE);
    }

    private volatile MetaHead metaHead = null;

    //声明队列
    private boolean declareQueue() {
        //只需要交换机和路由 不需要队列信息
        QueueInfo queue = getRetryTaskDeclareQueue();
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


    @Override
    protected MetaHead getMetaHead() {
        if (metaHead == null) {
            lock.lock();
            try {
                if (!declare.get()) {
                    bizLog.log(i -> log.info("队列还未声明绑定，先进行队列绑定"));
                    if (declareQueue()) {
                        bizLog.log(i -> log.info("队列声明成功！"));
                    } else {
                        bizLog.log(i -> log.error("队列声明失败！{}", getRetryTaskDeclareQueue()));
                    }
                    return null;
                }

                if (metaHead == null)
                    metaHead = new MetaHead()
                            .name("重试队列任务处理器")
                            .lock(lock)
                            .cacheList(sendList)
                            .executorPool(executor)
                            .dispatcherConfig(config)
                            .queueConfig(queueConfig)
                            .nacosService(nacosService)
                            .queue(getRetryTaskQueue())
                            .deliverPolicy(trackQueuePolicy)
                            .lastSendTime(lastSendTime)
                            .maxWaitTime(MAX_WAIT_TIME)
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
