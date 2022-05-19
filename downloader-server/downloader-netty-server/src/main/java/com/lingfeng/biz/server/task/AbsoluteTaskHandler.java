package com.lingfeng.biz.server.task;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.lingfeng.biz.downloader.log.BizLog;
import com.lingfeng.biz.downloader.model.*;
import com.lingfeng.biz.server.cache.WaterCacheQueue;
import com.lingfeng.biz.server.config.DispatcherConfig;
import com.lingfeng.biz.server.dispatcher.DispatcherRouter;
import com.lingfeng.biz.server.dispatcher.NodeClientStore;
import com.lingfeng.biz.server.model.NodeClient;
import com.lingfeng.biz.server.policy.DeliverPolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.CollectionUtils;
import store.StoreApi;

import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * @Author: wz
 * @Date: 2021/10/22 09:55
 * @Description: 调度中心核心调度器
 */
@Slf4j
public abstract class AbsoluteTaskHandler implements TaskHandler {

    protected DispatcherRouter<DownloadTask> dispatcherRouter = new DispatcherRouter<>();

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
        WaterCacheQueue<DownloadTask> cacheQueue = head.cacheQueue();
        try {
            if (!lock.tryLock(5000, TimeUnit.MILLISECONDS)) {
                return;
            }
            StoreApi<DownloadTask> storeApi = head.dbStore();
            //判断缓存是否应该 加入新的数据并加入新的数据
            boolean hungry = cacheQueue.isHungry();
            if (hungry) {
                List<DownloadTask> noJoinCacheList = cacheQueue.addSomeUntilFull(() -> {
                    //差值
                    int diff = cacheQueue.diff();
                    return pullData(diff);
                });
                //将不能分配的任务还原为“待下载”
                if (noJoinCacheList != null) {
                    for (DownloadTask item : noJoinCacheList) {
                        item.setStatus(TaskState.WAIT.code());
                        storeApi.updateById(item);
                    }
                }
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
            WaterCacheQueue<DownloadTask> cacheQueue = head.cacheQueue();
            DispatcherConfig dispatcherConfig = head.dispatcherConfig();
            Integer nodeMaxTaskCount = dispatcherConfig.getQueueLenLimit();
            //缓冲长度 (待发送数据长度)
            int total = cacheQueue.size();
            //没有要发送的数据
            if (total == 0) return true;
            String name = head.name();
            //获取分配策略
            DeliverPolicy<NodeRemain, Integer> deliverPolicy = head.deliverPolicy();
            //获取全部任务
            List<DownloadTask> taskList = cacheQueue.pollSome(cacheQueue.size());
            //省略任务其他属性只保留taskId,节约内存
            List<Integer> taskIdList = taskList.stream().map(DownloadTask::getId).collect(Collectors.toList());
            //获取路由中心
            DispatcherRouter router = SpringUtil.getBean(DispatcherRouter.class);
            //路由表
            ConcurrentMap<String, List<Route>> routePage = router.getRoutePage();
            //将路由表转成权重
            List<NodeRemain> nodeRemainList = new ArrayList<>();
            for (String clientId : routePage.keySet()) {
                NodeRemain nodeRemain = new NodeRemain();
                nodeRemain.setClientId(clientId);
                //节点剩余
                nodeRemain.setRemain(nodeMaxTaskCount - routePage.get(clientId).size());
                //设置节点最多消费个数
                nodeRemain.setMax(nodeMaxTaskCount);
                nodeRemainList.add(nodeRemain);
            }

            log.info("{} 通过{}策略进行任务分配", name, deliverPolicy.getClass());
            //通过策略进行任务分配
            // node --> list<taskId>
            Map<NodeRemain, List<Integer>> deliverData = deliverPolicy.deliver(taskIdList, nodeRemainList);
            if (!CollectionUtils.isEmpty(deliverData)) {
                //发送任务
                //deliver方法会减少sendList的个数 执行后的sendList.size()表示剩余没有发送的
                int sendCount = total - taskIdList.size();
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

    private List<NodeRemain> transRouteMap(Collection<List<Route>> routes) {

    }

    private void rollbackRetryQueueMsg(List<MsgTask> sendList) {
        Iterator<MsgTask> iterator = sendList.iterator();
        while (iterator.hasNext()) {
            MsgTask task = iterator.next();
            //note 回退节点给连接池

        }
    }


    /**
     * @Description: 从任务队列主动拉取数据并添加到缓冲中去
     * @return: void
     * @author: wz
     * @date: 2021/10/19 21:24
     */
    private List<DownloadTask> pullData(int count) {
        // ExecutorService executor = head.executorPool();
        //获取待执行队列信息
        //从存储中获取带处理的任务
        MetaHead metaHead = getMetaHead();
        StoreApi<DownloadTask> storeApi = metaHead.dbStore();
        //此处需要使用分布式锁,获取N个“待下载”的任务并修改为“下载中”
        return storeApi.queryAndModify(count, TaskState.WAIT.code(), TaskState.DOING.code());
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
        WaterCacheQueue<DownloadTask> cacheQueue = head.cacheQueue();
        try {
            lock.lock();
            //超时触发
            long now = System.currentTimeMillis();
            if (now - lastSendTime.get() >= maxWaitTime) {
                resetSendTime(head);
                return true;
            }
            //高水位发送
            if (cacheQueue.isEnough()) {
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
        ThreadPoolTaskExecutor senderExecutor = head.executorPool();
        final CountDownLatch latch = new CountDownLatch(sendCount);

        for (Map.Entry<QueueInfo, List<MsgTask>> item : result.entrySet()) {
            List<MsgTask> value = item.getValue();
            QueueInfo queue = item.getKey();
            l.log(i -> i.info("给{}发送{}条数据", queue.queue(), value.size()));
            for (MsgTask task : value) {
                try {

                    senderExecutor.execute(() -> {
                        boolean exception = false;
                        try {
                            String msg = task.getMsg();
                            //note MqSender.sendMsgWaitFor(msg, queue, 5000, null);
                        } catch (Exception e) {
                            AbsoluteTaskHandler.log.error(e.getMessage(), e);
                            //发送失败 为保证消息不丢失，手动nck
                            exception = true;
                            //note QueueChannel queueChannel = task.getQueueChannel();
                            //note MqUtil.nack(queueChannel.channel(), task.getDeliveryTag());
                        } finally {
                            latch.countDown();
                            //返回消息的ack，任务队列删除该消息
                            if (!exception) {
                                //note QueueChannel queueChannel = task.getQueueChannel();
                                //note MqUtil.ack(queueChannel.channel(), task.getDeliveryTag());
                            }
                        }
                    });
                } catch (Exception e) {
                    //线程池异常　如果任务提交失败
                    latch.countDown();
                    log.error(e.getMessage(), e);
                    //note QueueChannel queueChannel = task.getQueueChannel();
                    //note  MqUtil.nack(queueChannel.channel(), task.getDeliveryTag());
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
            //归还连接
            //note
            resetSendTime(head);
            l.log(i -> i.info("本次数据全部发送完毕!"));
        }
    }


    /**
     * @Description: 获取所有下载接节点当前的任务数 并按照空闲从小到大排列
     * @param: [head]
     * @return: java.util.List<com.lingfeng.biz.downloader.mq.entity.QueueInfo>
     * @author: wz
     * @date: 2021/10/28 18:48
     */
    public List<QueueInfo> searchQueueMsgCount(MetaHead head) {
        ReentrantLock lock = head.lock();
        try {
            lock.lock();
            DispatcherConfig config = head.dispatcherConfig();
            // NacosService nacos = head.nacosService();
            // QueueConfig queueConfig = head.queueConfig();
            //队列 正常消息 最大容量
            Integer queueLenLimit = config.getQueueLenLimit();
            //队列 重试消息 最大限制
            Integer retryLenLimit = config.getRetryLenLimit();
            int lastDice = head.lastDice();
            //全部队列都满的标记
            boolean allQueuesFull = true;
            //step1:获取所有的执行下载队列
            List<QueueInfo> allQueues = null;//note findAllDownloadExecuteQueue(nacos, queueConfig);
            List<QueueInfo> data = new ArrayList<>(allQueues != null ? allQueues.size() : 0);
            if (!CollectionUtil.isEmpty(allQueues)) {
                //step2:感知执行队列的任务数
                for (QueueInfo queue : allQueues) {
                    //队列当前总数
                    int queueLen = queue.queueLen();
                    if (queueLen == 0 || queueLen < (queueLenLimit + retryLenLimit)) {//如果API反馈任务是0 此时需要确认到底队列是不是0
                        queueLen = Math.max(queueLen, 0);//通过mq获取队列任务数(有延时)
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

    //获取所有的下载执行队列
    private List<QueueInfo> findAllDownloadExecuteQueue() {
//        List<NacosHost> allInstances = nacosNode.getAllInstances();
//        if (allInstances != null) {
//            return allInstances.stream().map(instance -> {
//                try {
//                    String nodeName = NacosService.clearInstanceId(instance.getInstanceId());
//                    //方案一：查询队列由于使用的是mq的监听线程所以此处获取的数据正好是消费者线程数N，与因为在任务下载完毕前，不会获取新的数据的
//                    int len = getDownloadNodeQueueLen(instance);
//                    return new QueueInfo()
//                            .queueLen(len)
//                            //队列名称 prefix + instanceId
//                            .queue(config.getExecuteQueuePrefix() + nodeName)
//                            //路由key instanceId
//                            .routeKey(nodeName)
//                            //交换机
//                            .exchange(config.getExecuteExchange());
//                } catch (Exception e) {
//                    log.error(e.getMessage(), e);
//                }
//                return null;
//            }).filter(Objects::nonNull).collect(Collectors.toList());
//        }
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
