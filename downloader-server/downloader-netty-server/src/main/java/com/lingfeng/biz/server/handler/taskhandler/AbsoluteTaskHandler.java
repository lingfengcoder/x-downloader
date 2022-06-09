package com.lingfeng.biz.server.handler.taskhandler;


import com.lingfeng.biz.downloader.model.DownloadTask;
import com.lingfeng.biz.downloader.model.NodeRemain;
import com.lingfeng.biz.downloader.model.QueueInfo;
import com.lingfeng.biz.downloader.model.RouteResult;
import com.lingfeng.biz.server.cache.CacheManage;
import com.lingfeng.biz.server.cache.WaterCacheQueue;
import com.lingfeng.biz.server.client.NodeClientGroup;
import com.lingfeng.biz.server.model.NodeClient;
import com.lingfeng.biz.server.route.Router;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author: wz
 * @Date: 2021/10/22 09:55
 * @Description: 调度中心核心调度器
 */
@Slf4j
public abstract class AbsoluteTaskHandler implements TaskHandler {
    private final static int DEFAULT_MAX_WAIT_TIME = 5000;//5s

    //获取调度头部信息
    protected abstract HandlerHead getHandlerHead();

    @Override
    public boolean handler() {
        HandlerHead handlerHead = getHandlerHead();
        if (handlerHead == null) return false;
        try {
            mainHandler(handlerHead);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return true;
    }

    //待执行任务监听
    private void mainHandler(HandlerHead head) {
        ReentrantLock lock = head.lock();
        try {
            if (!lock.tryLock(5000, TimeUnit.MILLISECONDS)) {
                return;
            }
            //判断缓存是否应该 加入新的数据并加入新的数据
            //处理缓冲队列的数据
            CacheManage.cacheProcess(head.dbStore(), head.cacheQueue());
            //路由任务
            Router instance = Router.getInstance();
            if (trigger(head)) {
                //路由分配结果
                RouteResult<NodeRemain, DownloadTask> route = instance.route(head.cacheQueue(), head.dispatcherConfig(), head.routePolicy());
                if (route != null) {
                    doRouteWork(route.routeMap(), route.count(), head);
                }
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    //判断是否达到发送消息的条件
    //   触发条件: 1.超过指定时间 2.缓冲队列已经满
    private boolean trigger(HandlerHead head) {
        ReentrantLock lock = head.lock();
        AtomicLong lastSendTime = head.lastSendTime();
        int maxWaitTime = head.maxWaitTime();
        maxWaitTime = maxWaitTime <= 0 ? DEFAULT_MAX_WAIT_TIME : maxWaitTime;
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
    private void resetSendTime(HandlerHead head) {
        ReentrantLock lock = head.lock();
        try {
            lock.lock();
            head.lastSendTime().set(System.currentTimeMillis());
        } finally {
            lock.unlock();
        }
    }


    //分发任务给指定队列
    private void doRouteWork(Map<NodeRemain, List<DownloadTask>> routePage,
                             int sendCount, HandlerHead head) {
        ThreadPoolTaskExecutor sendThreadGroup = head.executorPool();
        //客户组
        NodeClientGroup clientStore = NodeClientGroup.getInstance();
        //指定本次发送的锁
        final CountDownLatch latch = new CountDownLatch(sendCount);
        //对路由表进行迭代
        for (Map.Entry<NodeRemain, List<DownloadTask>> item : routePage.entrySet()) {
            submitThreadJob(item, sendThreadGroup, clientStore, latch);
        }
        //==================主线程等待所有子线程发送数据完毕==================================
        try {
            log.info("调度器主线程等待本批次消息发送中...");
            latch.await();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            log.info("本次数据全部发送完毕!");
        }
    }

    private void submitThreadJob(Map.Entry<NodeRemain, List<DownloadTask>> item,
                                 ThreadPoolTaskExecutor threadGroup,
                                 NodeClientGroup clientStore,
                                 final CountDownLatch latch) {
        List<DownloadTask> sendQueue = item.getValue();
        NodeRemain node = item.getKey();
        String clientId = node.getClientId();
        //获取目标客户端
        NodeClient client = clientStore.getClient(clientId);
        log.info("给{}发送{}条数据", clientId, sendQueue.size());
        for (DownloadTask task : sendQueue) {
            try {
                threadGroup.execute(() -> sendTask(task, client, latch));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                //note 如果线程池执行了拒绝策略，那么 latch锁依旧需要解锁
                latch.countDown();
            }
        }
    }

    //todo 发送消息失败 要重试 考虑是否增加重试列表
    // todo 这里需要重构，形成一个基于发送队列的生产者消费者发送模型(支持重试)，不能简单的直接使用线程池处理
    private void sendTask(DownloadTask task, NodeClient client, final CountDownLatch latch) {
        try {
            //发送任务
            SendApi.sendTaskToClient(client, task);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            latch.countDown();
        }
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
