package com.pukka.iptv.downloader.task;

import cn.hutool.core.date.SystemClock;
import cn.hutool.core.thread.ThreadUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.pukka.iptv.downloader.config.NodeConfig;
import com.pukka.iptv.downloader.model.DNode;
import com.pukka.iptv.downloader.model.Downloading;
import com.pukka.iptv.downloader.model.FileTask;
import com.pukka.iptv.downloader.model.resp.R;
import com.pukka.iptv.downloader.mq.config.RestHttp;
import com.pukka.iptv.downloader.mq.consumer.MqConsumer;
import com.pukka.iptv.downloader.mq.model.MsgTask;
import com.pukka.iptv.downloader.mq.model.QueueChannel;
import com.pukka.iptv.downloader.mq.model.QueueInfo;
import com.pukka.iptv.downloader.mq.pool.MqUtil;
import com.pukka.iptv.downloader.mq.pool.RabbitMqPool;
import com.pukka.iptv.downloader.nacos.listener.NacosNotify;
import com.pukka.iptv.downloader.nacos.model.NacosHost;
import com.pukka.iptv.downloader.task.process.AsyncDownloadProcess;
import com.pukka.iptv.downloader.threadpool.ThreadUtils;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
    private ScheduledThreadPoolExecutor downloaderScheduleThreadPool;
    @Autowired
    private AutoNode autoDNode;
    @Autowired
    private NodeConfig nodeConfig;
    @Autowired
    private AsyncDownloadProcess asyncDownloadProcess;
    private final List<MqConsumer> consumerList = new ArrayList<>();

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
            loopSchedule();
        }
        tmpIndexSchedule();
    }

    //定时执行
    private void loopSchedule() {
        if (ThreadUtils.isNullOrDone(scheduledFuture)) {
            scheduledFuture =
                    downloaderScheduleThreadPool.scheduleAtFixedRate(() -> {
                        loopHandlerWithTimeout(BALANCE_TIMEOUT);
                    }, 1, 5000, TimeUnit.MILLISECONDS);
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
            //关闭所有消费者
            closeAllListener();
            return;
        }
        if (!loopInitNodeWithTimeout(timeout)) return;
        //调整消费者线程
        loopBalanceConsumerWithTimeout(timeout);
    }

    //带超时时间的调整
    private void loopBalanceConsumerWithTimeout(long timeout) {
        QueueInfo queue = getConsumerQueue();
        if (queue == null) return;
        //连接池的连接数没有满足配额 或者 超时
        Integer limit = nodeConfig.getChannelLimit();
        //通过mq-api 获取消费者数量
        Integer consumerCount = MqUtil.getQueueConsumerCount(queue.queue());
        //先清理已经死亡的消费者
        clearDeathListener();
        if (limit > consumerCount) {
            addMqListener(queue, limit, timeout);
        } else if (limit < consumerCount) {
            //减少消费者
            reduceListener(consumerCount - limit);
        }
    }

    private void closeAllListener() {
        reduceListener(consumerList.size());
    }

    //补偿式 清除已经关闭的连接
    private void clearDeathListener() {
        Iterator<MqConsumer> iterator = consumerList.iterator();
        while (iterator.hasNext()) {
            MqConsumer next = iterator.next();
            //被动关闭
            Channel channel = next.channel();
            if (channel == null) continue;
            //主动关闭 如果channel 已经关闭了 直接移除就行
            if (!channel.isOpen()) {
                iterator.remove();
            }
        }
    }

    //减少消费者
    private void reduceListener(int delta) {
        Iterator<MqConsumer> iterator = consumerList.iterator();
        while (iterator.hasNext()) {
            MqConsumer next = iterator.next();
            //跳出循环
            if (delta <= 0) {
                break;
            }
            //被动关闭
            Channel channel = next.channel();
            if (channel == null) continue;
            if (!next.closed()) {
                if (channel.isOpen()) {
                    //手动设置关闭标识，让线程自己关闭
                    next.setCloseFlag();
                }
            } else {
                //主动关闭 如果channel 已经关闭了 直接提出就行
                if (!channel.isOpen()) {
                    iterator.remove();
                }
            }
            --delta;
        }
    }

    //增加消费者
    private void addMqListener(QueueInfo queue, int target, long timeout) {
        long time = SystemClock.now();
        boolean notTimeout = true;
        while (target > consumerList.size() && notTimeout) {
            MqConsumer mqConsumer = newMqListener(queue);
            if (mqConsumer != null) {
                consumerList.add(mqConsumer);
            }
            notTimeout = notTimeout(time, timeout);
        }
    }

    private MqConsumer newMqListener(QueueInfo queue) {
        //线程池执行(此处线程池非必须，真正执行的是mq内部维护的线程池)
        MqConsumer mqConsumer = new MqConsumer();
        boolean addOK = mqConsumer.name("下载器")
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
        if (addOK) {
            return mqConsumer;
        }
        return null;
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
            log.error(e.getMessage(), e);
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
        tmpIndexScheduleFuture = downloaderScheduleThreadPool
                .scheduleAtFixedRate(() -> Downloading.clearTmpIndex(liveTime),
                        1, liveTime, TimeUnit.MILLISECONDS);
    }

}
