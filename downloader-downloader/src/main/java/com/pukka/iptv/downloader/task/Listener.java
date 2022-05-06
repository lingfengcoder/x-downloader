package com.pukka.iptv.downloader.task;

import cn.hutool.core.date.SystemClock;
import cn.hutool.core.thread.ThreadUtil;
import com.alibaba.fastjson.JSON;
import com.pukka.iptv.downloader.config.NodeConfig;
import com.pukka.iptv.downloader.model.DNode;
import com.pukka.iptv.downloader.model.Downloading;
import com.pukka.iptv.downloader.model.FileTask;
import com.pukka.iptv.downloader.mq.consumer.MqConsumer;
import com.pukka.iptv.downloader.mq.model.MsgTask;
import com.pukka.iptv.downloader.mq.model.QueueChannel;
import com.pukka.iptv.downloader.mq.model.QueueInfo;
import com.pukka.iptv.downloader.mq.pool.MqUtil;
import com.pukka.iptv.downloader.mq.pool.RabbitMqPool;
import com.pukka.iptv.downloader.nacos.listener.NacosNotify;
import com.pukka.iptv.downloader.task.process.AsyncDownloadProcess;
import com.pukka.iptv.downloader.threadpool.ThreadUtils;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @Author: wz
 * @Date: 2022/1/4 20:54
 * @Description: 根据配置动态创建或销毁mq监听
 */
@Slf4j
@Component
@Order(2)
public class Listener implements NacosNotify {
    @Resource(name = "downloaderScheduleThreadPool")
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
    @Autowired
    private AutoNode autoDNode;
    @Autowired
    private NodeConfig nodeConfig;
    @Autowired
    private AsyncDownloadProcess asyncDownloadProcess;


    private static volatile DNode node;

    private volatile ScheduledFuture<?> scheduledFuture = null;

    private volatile ScheduledFuture<?> tmpIndexScheduleFuture = null;

    private final static long BALANCE_TIMEOUT = 60000L;//配置调整超时时间30s


    //下载节点初始化
    @PostConstruct
    private void init() {
        //主线程定时执行 每秒执行一次
        if (nodeConfig.isEnable()) {
            loopSchedule();
        }
        tmpIndexSchedule();
    }


    @Override//NACOS 配置改变进行处理
    public void configRefreshEvent() {
        if (nodeConfig.isEnable()) {
            //根据配置调整当前key对应的连接池限制
            balanceListener();
            loopSchedule();
        }
        tmpIndexSchedule();
    }

    //定时执行
    private void loopSchedule() {
        if (ThreadUtils.isNullOrDone(scheduledFuture)) {
            scheduledFuture =
                    scheduledThreadPoolExecutor.scheduleAtFixedRate(() -> {
                        loopHandlerWithTimeout(BALANCE_TIMEOUT);
                    }, 1, 1000, TimeUnit.MILLISECONDS);
        }
    }

    //带超时时间的初始化下载节点
    private boolean loopInitNodeWithTimeout(long timeout) {
        long time = SystemClock.now();
        //此处做超时等待处理，可能存在nacos注册有延时问题
        while (getConsumerQueue() == null && notTimeout(time, timeout)) {
            ThreadUtil.sleep(500);
        }
        if (getConsumerQueue() == null) {
            log.error("生成下载节点失败！请更新nacos配置触发 节点自动生成{}", getDNode());
            return false;
        }
        return true;
    }

    //监听mq消息并执行下载任务
    public void loopHandlerWithTimeout(long timeout) {
        if (!nodeConfig.isEnable()) {
            return;
        }
        if (!loopInitNodeWithTimeout(timeout)) return;
        //调整消费者线程
        loopBalanceConsumerWithTimeout(timeout);
    }

    //根据配置调整当前key对应的连接池限制
    private void balanceListener() {
        QueueInfo queue = getConsumerQueue();
        //连接池的连接数没有满足配额 或者 超时
        Integer limit = nodeConfig.getChannelLimit();
        //使用循环　在增加连接数的情况下，会在限定时间不停内向连接中申请新的连接
        RabbitMqPool.RKey key = RabbitMqPool.RKey.newKey(queue, limit);
        //此处使用真实的大小而不是配置大小，因为需要根据期望配置不停的去调整连接池，创建或者剔除新的监听者
        int realSize = RabbitMqPool.me().getPoolRealBusyNodeSize(key);
        //如果配置有调整
        if (limit != realSize) {
            //调整配置 主要作用在减少的情境下，在归还之后会进行关闭
            RabbitMqPool.me().balanceKeyPoolSize(key);
        }
    }

    //带超时时间的调整
    private void loopBalanceConsumerWithTimeout(long timeout) {
        QueueInfo queue = getConsumerQueue();
        if (queue == null) return;
        long time = SystemClock.now();
        boolean notTimeout = true;
        //连接池的连接数没有满足配额 或者 超时
        Integer limit = nodeConfig.getChannelLimit();
        RabbitMqPool.RKey key = RabbitMqPool.RKey.newKey(queue);
        //读写分离线程不安全
        //如果keyPool对应的连接数没有达到 配置的数量，则不停的提交新的消费者，形成新的连接
        while (limit > RabbitMqPool.me().getPoolRealBusyNodeSize(key) && notTimeout) {
            ThreadUtil.sleep(300);
            addMqListener(queue);
            notTimeout = notTimeout(time, timeout);
        }
        //如果没有超时，说明调整的配置都操作完毕了，可以取消定时任务了
        if (notTimeout) {
            ThreadUtils.cancelSchedule(scheduledFuture);
        }
    }

    private void addMqListener(QueueInfo queue) {
        //线程池执行(此处线程池非必须，真正执行的是mq内部维护的线程池)
        new MqConsumer().name("下载器")
                .mq(queue).autoAck(false)
                .work(msgTask -> {
                    //如果下载节点被关闭，直接nack
                    if (!nodeConfig.isEnable()) {
                        nackTask(msgTask);
                        return true;
                    }
                    //解析数据
                    FileTask fileTask = parseMsg(msgTask);
                    //开启下载
                    asyncDownloadProcess.download(msgTask, fileTask);
                    return true;
                }).listen();
    }

    //解析msg
    private FileTask parseMsg(MsgTask msgTask) {
        String msg = msgTask.getMsg();
        return Optional.ofNullable(JSON.parseObject(msg, FileTask.class)).orElseThrow(() -> {
            log.error("RabbitMQ消息解析失败-{}", msg);
            return new RuntimeException("RabbitMQ消息解析失败");
        });
    }

    private boolean notTimeout(long begin, long timeout) {
        return SystemClock.now() - begin < timeout;
    }

    public void nackTask(MsgTask msgTask) {
        try {
            //防止频繁nack造成资源浪费
            Thread.sleep(100);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        //节点没有初始化，需要退回消息
        QueueChannel queueChannel = msgTask.getQueueChannel();
        Channel channel = queueChannel.channel();
        MqUtil.nack(channel, msgTask.getDeliveryTag());
    }

    public void ackTask(MsgTask msgTask) {
        //节点没有初始化，需要退回消息
        QueueChannel queueChannel = msgTask.getQueueChannel();
        Channel channel = queueChannel.channel();
        MqUtil.ack(channel, msgTask.getDeliveryTag());
    }


    //初始化当前下载节点
    public DNode getDNode() {
        return node == null ? node = autoDNode.defaultDownloadNode() : node;
    }

    //获取下载器的监听队列
    private QueueInfo getConsumerQueue() {
        //消费者不需要知道交换机和路由
        DNode dNode = getDNode();
        return dNode != null ? dNode.getQueueInfo().exchange(null).routeKey(null) : null;
    }

    //临时下载文件的 定时清理器
    private void tmpIndexSchedule() {
        // log.info("getTmpIndexTimeCron={}", nodeConfig.getTmpIndexTimeCron());
        log.info("【tmpIndexSchedule】设置清理临时下载文件的周期时间={}", nodeConfig.getTmpIndexLiveTime());
        Long liveTime = nodeConfig.getTmpIndexLiveTime();
        if (tmpIndexScheduleFuture != null) {
            ThreadUtils.cancelSchedule(tmpIndexScheduleFuture);
        }
        tmpIndexScheduleFuture = scheduledThreadPoolExecutor
                .scheduleAtFixedRate(() -> Downloading.clearTmpIndex(liveTime),
                        1, liveTime, TimeUnit.MILLISECONDS);
    }

}
