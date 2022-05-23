package com.lingfeng.biz.server.task;

import com.lingfeng.biz.downloader.log.BizLog;
import com.lingfeng.biz.downloader.model.*;
import com.lingfeng.biz.server.cache.WaterCacheQueue;
import com.lingfeng.biz.server.config.DispatcherConfig;
import com.lingfeng.biz.server.dispatcher.DispatcherRouter;
import com.lingfeng.biz.server.dispatcher.NodeClientGroup;
import com.lingfeng.biz.server.model.NodeClient;
import com.lingfeng.biz.server.policy.DeliverPolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * @Author: wz
 * @Date: 2022/5/20 13:46
 * @Description:
 */

@Slf4j
public class TaskDispatcher {
    private static final TaskDispatcher instance = new TaskDispatcher();

    public static TaskDispatcher getInstance() {
        return instance;
    }

    //分配任务
    public boolean dispatch(MetaHead head) {
        ReentrantLock lock = head.lock();
        BizLog l = head.log();
        try {
            //5s超时时间
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
            DeliverPolicy<NodeRemain, DownloadTask> deliverPolicy = head.deliverPolicy();
            //获取全部任务 //todo 可以根据路由器的空闲情况获取指定数量的
            List<DownloadTask> taskList = cacheQueue.pollSome(10);
            //获取路由中心
            DispatcherRouter<DownloadTask> router = DispatcherRouter.getInstance();
            //客户组
            NodeClientGroup clientStore = NodeClientGroup.getInstance();
            //路由表
            ConcurrentMap<String, List<Route<DownloadTask>>> routePage = router.getRoutePage();
            //将路由表转成权重
            List<NodeRemain> nodeRemainList = new ArrayList<>();
            for (String clientId : routePage.keySet()) {
                NodeClient client = clientStore.getClient(clientId);
                //note 只有活跃的客户端才能参与本次路由
                if (!client.isAlive()) continue;
                NodeRemain nodeRemain = new NodeRemain();
                nodeRemain.setClientId(clientId);
                //节点剩余
                nodeRemain.setRemain(nodeMaxTaskCount - routePage.get(clientId).size());
                //设置节点最多消费个数
                nodeRemain.setMax(nodeMaxTaskCount);
                nodeRemainList.add(nodeRemain);
            }
            //通过策略进行任务分配
            log.info("{} 通过{}策略进行任务分配", name, deliverPolicy.getClass());
            // node --> list<taskId>
            //发送的消息进入路由表，未发送的消息退回cache
            //deliver方法会减少sendList的个数 执行后的sendList.size()表示剩余没有发送的
            Map<NodeRemain, List<DownloadTask>> deliverData = deliverPolicy.deliver(taskList, nodeRemainList);

            //note 不需要给node节点分配的数据,则退回给缓存队列
            for (DownloadTask downloadTask : taskList) {
                //强制添加，可能会高出高水位
                cacheQueue.addMust(downloadTask);
            }

            //添加路由信息 (将任务从cache中移动到路由表中)
            for (NodeRemain node : deliverData.keySet()) {
                String clientId = node.getClientId();
                List<DownloadTask> sendQueue = deliverData.get(node);
                List<Route<DownloadTask>> routes =
                        sendQueue.stream().map(t -> {
                            Route<DownloadTask> route = new Route<>();
                            route.setId(t.getId().toString());
                            route.setTarget(clientId);
                            route.setData(t);
                            return route;
                        }).collect(Collectors.toList());
                router.addRoute(clientId, routes);
            }

            //发送任务
            if (!CollectionUtils.isEmpty(deliverData)) {
                int sendCount = (total - taskList.size());
                log.info("{} 本次将发送{}条数据", name, sendCount);
                doSendTask(deliverData, sendCount, head);
                return true;
            }
            return false;
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        } finally {
            lock.unlock();
        }
        return false;
    }

    //分发任务给指定队列
    private void doSendTask(Map<NodeRemain, List<DownloadTask>> routePage,
                            int sendCount, MetaHead head) {
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

    private void submitThreadJob(Map.Entry<NodeRemain, List<DownloadTask>> item, ThreadPoolTaskExecutor threadGroup, NodeClientGroup clientStore,
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
            SendeApi.sendTaskToClient(client, task);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            latch.countDown();
        }
    }


}
