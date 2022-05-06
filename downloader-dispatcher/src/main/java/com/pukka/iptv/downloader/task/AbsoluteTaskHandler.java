package com.pukka.iptv.downloader.task;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONObject;
import com.pukka.iptv.downloader.config.DispatcherConfig;
import com.pukka.iptv.downloader.log.BizLog;
import com.pukka.iptv.downloader.model.resp.R;
import com.pukka.iptv.downloader.mq.config.QueueConfig;
import com.pukka.iptv.downloader.mq.config.RestHttp;
import com.pukka.iptv.downloader.mq.consumer.MqPullConsumer;
import com.pukka.iptv.downloader.mq.model.MsgTask;
import com.pukka.iptv.downloader.mq.model.QueueChannel;
import com.pukka.iptv.downloader.mq.model.QueueInfo;
import com.pukka.iptv.downloader.mq.pool.MqUtil;
import com.pukka.iptv.downloader.mq.pool.RabbitMqPool;
import com.pukka.iptv.downloader.mq.producer.MqSender;
import com.pukka.iptv.downloader.nacos.NacosService;
import com.pukka.iptv.downloader.nacos.model.NacosHost;
import com.pukka.iptv.downloader.policy.DeliverPolicy;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * @Author: wz
 * @Date: 2021/10/22 09:55
 * @Description: 调度中心核心调度器
 */
@Slf4j
public abstract class AbsoluteTaskHandler implements TaskHandler {


    //获取调度头部信息
    protected abstract MetaHead getMetaHead();

    @Override
    public boolean handler() {
        MetaHead metaHead = getMetaHead();
        if (metaHead == null) return false;
        try {
            handler(metaHead);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return true;
    }

    //待执行任务监听
    private void handler(MetaHead head) {
        ReentrantLock lock = head.lock();
        List<MsgTask> msgTasks = head.cacheList();

        BizLog l = head.log();
        try {
            if (!lock.tryLock(5000, TimeUnit.MILLISECONDS)) {
                return;
            }
            // BizLog.logs(i -> log.info(head.name() + " 开始执行"));
            l.log(i -> log.info(head.name() + " 开始执行"));
            //如果队列为空的要主动拉一下
            //获取差值
            int diff = head.dispatcherConfig().getCacheTaskLen() - msgTasks.size();
            if (diff > 0) {
                //拉diff个消息
                head.queue().fetchCount(diff);
                //主动拉取数据
                pullData(head);
            }
            //处理缓冲队列的数据
            if (trigger(head)) {
                refreshCacheAndSendTask(head);
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }


    /**
     * @Description: 刷新缓存队列并发送消息
     * @param: [head]
     * @return: boolean
     * @author: wz
     * @date: 2021/10/28 18:42
     */
    private boolean refreshCacheAndSendTask(MetaHead head) {
        ReentrantLock lock = head.lock();
        BizLog l = head.log();
        try {
            if (!lock.tryLock(5000, TimeUnit.MILLISECONDS)) {
                return false;
            }
            List<MsgTask> sendList = head.cacheList();
            //缓冲长度 (待发送数据长度)
            int total = sendList.size();
            //没有要发送的数据
            if (total == 0) return true;
            String name = head.name();
            //获取分配策略
            DeliverPolicy<QueueInfo, MsgTask> deliverPolicy = head.deliverPolicy();
            //获取空闲队列
            List<QueueInfo> freeQueue = searchQueueMsgCount(head);
            l.log(i -> log.info("{} 通过{}策略进行任务分配", name, deliverPolicy.getClass()));
            //通过策略进行任务分配
            Map<QueueInfo, List<MsgTask>> deliverData = deliverPolicy.deliver(sendList, freeQueue);
            if (!CollectionUtils.isEmpty(deliverData)) {
                //发送任务
                //deliver方法会减少sendList的个数 执行后的sendList.size()表示剩余没有发送的
                int sendCount = total - sendList.size();
                l.log(i -> log.info("{} 共送{}条数据", name, total));
                l.log(i -> log.info("{} 本次将发送{}条数据", name, sendCount));
                sendTaskToExecuteQueue(deliverData, sendCount, head);
                return true;
            } else {
                //如果本次的任务一个也没有发出去，需要将
                //如果重试队列的数据 反给重试队列 避免重试队列都是 同一个队列的数据阻塞
                rollbackRetryQueueMsg(sendList);
                return false;
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        } finally {
            lock.unlock();
        }
        return false;
    }

    private void rollbackRetryQueueMsg(List<MsgTask> sendList) {
        Iterator<MsgTask> iterator = sendList.iterator();
        while (iterator.hasNext()) {
            MsgTask task = iterator.next();
            if (task.isRetryMsg()) {
                iterator.remove();
                //如果重试队列的数据 反给重试队列
                QueueChannel queueChannel = task.getQueueChannel();
                MqUtil.nack(queueChannel.channel(), task.getDeliveryTag());
            }
        }
    }

    //并发控制
    private void concurrentControl(MetaHead head) {
        QueueInfo queue = head.queue();
        DispatcherConfig config = head.dispatcherConfig();
        //从配置中获取并发限制数 (单个key的指定配置)
        Integer limit = config.getChannelLimit();
        Boolean enable = config.getEnable();
        if (!enable) {
            limit = 0;
        }
        //重新设置 key对应管道队列的最大连接数
        RabbitMqPool.RKey key = RabbitMqPool.RKey.newKey(queue, limit);
        RabbitMqPool.me().balanceKeyPoolSize(key);
    }

    //添加任务到缓冲区
    private boolean addCacheQueue(MetaHead head, MsgTask msgTask) {
        ReentrantLock lock = head.lock();
        try {
            lock.lock();
            //如果队列未满
            if (head.cacheList().size() < head.dispatcherConfig().getCacheTaskLen()) {
                head.cacheList().add(msgTask);
                return true;
            }
        } finally {
            lock.unlock();
        }
        return false;
    }

    /**
     * @Description: 从任务队列主动拉取数据并添加到缓冲中去
     * @return: void
     * @author: wz
     * @date: 2021/10/19 21:24
     */
    private void pullData(MetaHead head) {
        // ExecutorService executor = head.executorPool();
        //获取待执行队列信息
        QueueInfo queue = head.queue();
        //并发控制
        concurrentControl(head);
        //主动拉取数据
        //l.log(i -> i.info("{} 主动拉取数据", head.name()));
        new MqPullConsumer()
                .name(head.name())
                .mq(queue)
                .autoAck(false)//不自动ack
                .autoBackChannel(false)//不自动连接节点归还
                .work(msgTask -> {
                    //添加到缓冲队列中
                    boolean addSuccess = addCacheQueue(head, msgTask);
                    if (!addSuccess) {
                        //没有添加成功的退回给mq
                        log.warn("没有添加成功的退回给mq");
                        Channel channel = msgTask.getQueueChannel().channel();
                        MqUtil.nack(channel, msgTask.getDeliveryTag());
                    }
                    //判断是否触发 发送条件
                    // if (trigger(head)) refreshCacheAndSendTask(head);
                    return true;
                }).pull();
    }


    /**
     * @Description: 判断是否达到发送消息的条件
     * 触发条件: 1.超过指定时间 2.缓冲队列已经满
     * @return: boolean
     * @author: wz
     * @date: 2021/10/19 21:24
     */
    private boolean trigger(MetaHead head) {
        ReentrantLock lock = head.lock();
        AtomicLong lastSendTime = head.lastSendTime();
        int maxWaitTime = head.maxWaitTime();
        //通过nacos动态获取到处理器信息
        DispatcherConfig config = head.dispatcherConfig();
        List<MsgTask> sendList = head.cacheList();
        try {
            lock.lock();
            //超时触发
            long now = System.currentTimeMillis();
            if (now - lastSendTime.get() >= maxWaitTime) {
                resetSendTime(head);
                return true;
            }
            //队列大于最大任务数
            if (sendList.size() >= config.getCacheTaskLen()) {
                return true;
            }
        } finally {
            lock.unlock();
        }
        return false;
    }

    //重置发布时间
    private void resetSendTime(MetaHead head) {
        ReentrantLock lock = head.lock();
        try {
            lock.lock();
            head.lastSendTime().set(System.currentTimeMillis());
        } finally {
            lock.unlock();
        }
    }

    //分发任务给指定队列
    private void sendTaskToExecuteQueue(Map<QueueInfo, List<MsgTask>> result, int sendCount, MetaHead head) {
        BizLog l = head.log();
        ExecutorService senderExecutor = head.executorPool();
        final CountDownLatch latch = new CountDownLatch(sendCount);
        //发送任务
        HashSet<QueueChannel> needBackList = new HashSet<>();
        for (Map.Entry<QueueInfo, List<MsgTask>> item : result.entrySet()) {
            List<MsgTask> value = item.getValue();
            QueueInfo queue = item.getKey();
            l.log(i -> i.info("给{}发送{}条数据", queue.queue(), value.size()));
            for (MsgTask task : value) {
                try {
                    //阻塞式发送消息
                    needBackList.add(task.getQueueChannel());
                    senderExecutor.execute(() -> {
                        boolean exception = false;
                        try {
                            String msg = task.getMsg();
                            MqSender.sendMsgWaitFor(msg, queue, 5000, null);
                        } catch (Exception e) {
                            AbsoluteTaskHandler.log.error(e.getMessage(), e);
                            //发送失败 为保证消息不丢失，手动nck
                            exception = true;
                            QueueChannel queueChannel = task.getQueueChannel();
                            MqUtil.nack(queueChannel.channel(), task.getDeliveryTag());
                        } finally {
                            latch.countDown();
                            //返回消息的ack，任务队列删除该消息
                            if (!exception) {
                                QueueChannel queueChannel = task.getQueueChannel();
                                MqUtil.ack(queueChannel.channel(), task.getDeliveryTag());
                            }
                        }
                    });
                } catch (Exception e) {
                    //线程池异常　如果任务提交失败
                    latch.countDown();
                    log.error(e.getMessage(), e);
                    QueueChannel queueChannel = task.getQueueChannel();
                    MqUtil.nack(queueChannel.channel(), task.getDeliveryTag());
                }
            }
        }
        try {
            l.log(i -> i.info("调度器等待本批次消息发送中..."));
            latch.await();
            //ThreadUtils.waitForFuture(futureList);
        } catch (Exception e) {
            l.log(i -> i.error(e.getMessage(), e));
        } finally {
            resetSendTime(head);
            //归还连接
            for (QueueChannel item : needBackList) {
                RabbitMqPool.me().back(item.node());
            }
            l.log(i -> i.info("本次数据全部发送完毕!"));
        }
    }


    /**
     * @Description: 获取所有下载接节点当前的任务数 并按照空闲从小到大排列
     * @param: [head]
     * @return: java.util.List<com.pukka.iptv.downloader.mq.entity.QueueInfo>
     * @author: wz
     * @date: 2021/10/28 18:48
     */
    public List<QueueInfo> searchQueueMsgCount(MetaHead head) {
        ReentrantLock lock = head.lock();
        try {
            lock.lock();
            DispatcherConfig config = head.dispatcherConfig();
            NacosService nacos = head.nacosService();
            QueueConfig queueConfig = head.queueConfig();
            //队列 正常消息 最大容量
            Integer queueLenLimit = config.getQueueLenLimit();
            //队列 重试消息 最大限制
            Integer retryLenLimit = config.getRetryLenLimit();
            int lastDice = head.lastDice();
            //全部队列都满的标记
            boolean allQueuesFull = true;
            //step1:获取所有的执行下载队列
            List<QueueInfo> allQueues = findAllDownloadExecuteQueue(nacos, queueConfig);
            List<QueueInfo> data = new ArrayList<>(allQueues != null ? allQueues.size() : 0);
            if (!CollectionUtil.isEmpty(allQueues)) {
                //step2:感知执行队列的任务数
                for (QueueInfo queue : allQueues) {
                    //队列当前总数
                    int queueLen = queue.queueLen();
                    if (queueLen == 0) {//如果API反馈任务是0 此时需要确认到底队列是不是0
                        queueLen = MqUtil.getQueueLen(queue.queue());//通过mq获取队列任务数(有延时)
                    }
                    //如果队列长度小于0说明获取队列信息失败，不进行分配任务
                    if (queueLen < 0) {
                        continue;
                    }
                    //下载队列长队+重试队列长度=总空闲长度
                    int remain = queueLenLimit + retryLenLimit - queueLen;
                    //如果没有空位就直接继续循环
                    if (remain <= 0) continue;
                    allQueuesFull = false; //如果有一个队列有空余，标志设置false
                    //通过标记重置优先级的方式进行分配(例如：本次优先下载队列分配，下次优先重试队列分配)
                    throwDice(queue, remain, queueLenLimit, retryLenLimit, lastDice);
                    //添加到结果集中
                    data.add(queue);
                }
            }
            //再次抛硬币
            head.throwDice();
            //剩余数 从小到大排列
            CollectionUtil.sort(data, Comparator.comparingInt(QueueInfo::remainCount));
            //如果所有的队列都满了 返回null
            return allQueuesFull ? null : data;
        } finally {
            lock.unlock();
        }
    }

    //获取下载节点的队列长度
    private int getDownloadNodeQueueLen(NacosHost host) {
        String ip = host.getIp();
        int port = host.getPort();
        try {
            RestTemplate rest = RestHttp.getRestHttp();
            String url = "http://" + ip + ":" + port + "/api/download/queueLen";
            ResponseEntity<String> resp = rest.getForEntity(url, String.class);
            if (resp.getStatusCode() == HttpStatus.OK) {
                R<Integer> data = JSONObject.parseObject(resp.getBody(), R.class);
                if (data != null) {
                    return data.getData();
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return -1;
    }

    //获取所有的下载执行队列
    private List<QueueInfo> findAllDownloadExecuteQueue(NacosService nacosNode, QueueConfig config) {
        List<NacosHost> allInstances = nacosNode.getAllInstances();
        if (allInstances != null) {
            return allInstances.stream().map(instance -> {
                try {
                    String nodeName = NacosService.clearInstanceId(instance.getInstanceId());
                    //方案一：查询队列由于使用的是mq的监听线程所以此处获取的数据正好是消费者线程数N，与因为在任务下载完毕前，不会获取新的数据的
                    int len = getDownloadNodeQueueLen(instance);
                    return new QueueInfo()
                            .queueLen(len)
                            //队列名称 prefix + instanceId
                            .queue(config.getExecuteQueuePrefix() + nodeName)
                            //路由key instanceId
                            .routeKey(nodeName)
                            //交换机
                            .exchange(config.getExecuteExchange());
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
                return null;
            }).filter(Objects::nonNull).collect(Collectors.toList());
        }
        return null;
    }

    //掷色子决定谁的优先级更高(可以用随机数)
    //如果下载limit=8 重试limit=2  最坏情况(每次空闲1个) 5:5 最好情况 8:2
    private int throwDice(QueueInfo queue, int remain, int queueLimit, int retryLenLimit, int last) {
        //如果上次下载队列优先级高，这次重试队列优先级高
        if (last == 0) {
            //如果剩余空位大于 重试限制，优先重试队列
            // 例如  重试limit=2 下载limit=10 空余10 个位置  ==> 重试分2个 下载分8个
            if (remain >= retryLenLimit) {
                queue.retryCount(retryLenLimit);
                //剩下的位置都是正常任务的位置
                queue.remainCount(remain - retryLenLimit);
            } else {
                //如果剩余位置不够,优先重试队列
                //例如 重试limit=2 下载limit=10 空余1 个位置 ==> 重试分1个 下载分0个
                queue.retryCount(remain);
                queue.remainCount(0);
            }
            //返回这次优先的模式: 重试队列
            return 1;
        } else {
            //否则下载队列优先级高
            if (remain >= queueLimit) {
                //优先把下载队列填满
                queue.remainCount(queueLimit);
                //剩下的位置才是重试任务的
                queue.retryCount(remain - queueLimit);
            } else {
                //如果剩余位置不够,优先重试队列
                //例如 重试limit=2 下载limit=10 空余7 个位置 ==> 重试分0个 下载分7个
                queue.remainCount(remain);
                queue.retryCount(0);
            }
            //返回这次优先的模式: 下载队列
            return 0;
        }
    }
}
