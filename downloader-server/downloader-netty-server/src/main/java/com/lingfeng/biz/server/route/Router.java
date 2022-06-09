package com.lingfeng.biz.server.route;

import com.lingfeng.biz.downloader.model.DownloadTask;
import com.lingfeng.biz.downloader.model.NodeRemain;
import com.lingfeng.biz.downloader.model.Route;
import com.lingfeng.biz.downloader.model.RouteResult;
import com.lingfeng.biz.server.cache.WaterCacheQueue;
import com.lingfeng.biz.server.client.NodeClientGroup;
import com.lingfeng.biz.server.config.DispatcherConfig;
import com.lingfeng.biz.server.model.NodeClient;
import com.lingfeng.biz.server.policy.RoutePolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * @Author: wz
 * @Date: 2022/5/26 09:54
 * @Description:
 */

@Slf4j
public class Router {

    public static Router getInstance() {
        return singleton.router;
    }

    private static class singleton {
        private final static Router router = new Router();
    }

    //路由任务
    public RouteResult<NodeRemain, DownloadTask> route(WaterCacheQueue<DownloadTask> cacheQueue,
                                                       DispatcherConfig dispatcherConfig,
                                                       RoutePolicy<NodeRemain, DownloadTask> routePolicy) {
        try {
            Integer nodeMaxTaskCount = dispatcherConfig.getTaskLimit();
            //缓冲长度 (待发送数据长度)
            int total = cacheQueue.size();
            //没有要发送的数据
            if (total == 0) return null;
            //获取全部任务 //todo 可以根据路由器的空闲情况获取指定数量的
            List<DownloadTask> taskList = cacheQueue.pollSome(10);
            //获取路由存储
            RouterStore<DownloadTask> router = RouterStore.getInstance();
            //客户组
            NodeClientGroup clientStore = NodeClientGroup.getInstance();
            //路由表
            ConcurrentMap<String, List<Route<DownloadTask>>> routePage = router.getRoutePage();
            //将路由表转成权重
            List<NodeRemain> nodeRemainList = new ArrayList<>();
            Collection<NodeClient> clients = clientStore.getClients();
            for (NodeClient client : clients) {
                //note 只有活跃的客户端才能参与本次路由
                if (!client.isAlive()) continue;
                NodeRemain nodeRemain = new NodeRemain();
                nodeRemain.setClientId(client.getClientId());
                //节点剩余
                List<Route<DownloadTask>> routes = routePage.get(client.getClientId());
                nodeRemain.setRemain(nodeMaxTaskCount - (routes == null ? 0 : routes.size()));
                //设置节点最多消费个数
                nodeRemain.setMax(nodeMaxTaskCount);
                nodeRemainList.add(nodeRemain);
            }
            //通过策略进行任务分配
            log.info(" 通过{}策略进行任务分配", routePolicy.getClass());
            // node --> list<taskId>
            //发送的消息进入路由表，未发送的消息退回cache
            //deliver方法会减少sendList的个数 执行后的sendList.size()表示剩余没有发送的
            Map<NodeRemain, List<DownloadTask>> deliverData = routePolicy.deliver(taskList, nodeRemainList);

            //note 不需要给node节点分配的数据,则退回给缓存队列
            for (DownloadTask downloadTask : taskList) {
                //强制添加，可能会高出高水位
                cacheQueue.addMust(downloadTask);
            }

            //添加路由信息 (将任务从cache中移动到路由表中)
            if (deliverData != null) {
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
            }
            //发送任务
            if (!CollectionUtils.isEmpty(deliverData)) {
                int sendCount = (total - taskList.size());
                log.info(" 本次将路由{}条数据", sendCount);
                return new RouteResult<>(deliverData, sendCount);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }
}
