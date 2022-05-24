package com.lingfeng.biz.server.handler;

import cn.hutool.extra.spring.SpringUtil;
import com.lingfeng.biz.downloader.model.*;
import com.lingfeng.biz.server.cache.WaterCacheQueue;
import com.lingfeng.biz.server.dispatcher.DispatcherRouter;
import com.lingfeng.biz.server.task.NormalTaskHandler;
import com.lingfeng.dutation.store.StoreApi;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


/**
 * @Author: wz
 * @Date: 2022/5/18 19:44
 * @Description: note 消息处理器需要注意的点: 1.重复的消息处理 2.
 */
@Setter
@Getter
@Builder
@Slf4j
public class TaskHandler implements Runnable {

    private TaskFrame<DownloadTask> taskFrame;

    @Override
    public void run() {
        String taskId = taskFrame.getTaskId();
        TaskCmd taskCmd = taskFrame.getTaskCmd();
        DownloadTask task = taskFrame.getData();
        String clientId = taskFrame.getClientId();
        //获取路由中心
        DispatcherRouter<DownloadTask> router = DispatcherRouter.getInstance();
        Route<DownloadTask> route = router.getRoute(clientId, taskId);
        if (route == null) {
            log.error("路由表不存在该记录{}", taskFrame);
            return;
        }
        StoreApi<DownloadTask> storeApi = SpringUtil.getBean(StoreApi.class);
        switch (taskCmd) {
            //###### 任务完成 ######
            // 1.将任务在路由表中更新为完成 ，并在持久层更新任务  2.将任务从路由表删除
            case TASK_FIN:
                if (task.getId() == null) {
                    log.error("缺少task id task={}", task);
                    return;
                }
                task.setStatus(TaskState.FIN.code());
                //先把路由器中的数据更新为完成的状态 防止持久层失败，可以用于重试持久层(待开发)
                DownloadTask routeData = route.getData();
                routeData.setStatus(task.getStatus());
                //更新任务状态
                if (storeApi.updateById(task)) {
                    //删除路由节点
                    router.removeRouteNode(clientId, taskId);
                }
                break;
            //######拒绝任务######

            // 两种实现方式
            // 1.将任务从路由表中删除，并将任务变为 待执行 同步到持久层，该任务会通过"饥饿算法"再次进入cache准备执行
            // (缺点：任务周转周期较长； 优点：任务状态形成了闭环，不会造成cache溢出等问题)
            //
            // 2.直接将任务从路由表转移到cache中 用于从新再次分配 任务状态不做变化
            // （优点：任务周转周期较短优先级任务处理速度明显； 缺点：可能存在cache溢出问题(不严重)）
            case TASK_REJECT:
                //这里采用方案2实现
                //cache
                NormalTaskHandler normalTaskHandler = SpringUtil.getBean(NormalTaskHandler.class);
                WaterCacheQueue<DownloadTask> cacheQueue = normalTaskHandler.getCacheQueue();
                if (router.removeRouteNode(clientId, taskId)) {
                    //添加给cache 用于再次分配
                    cacheQueue.addMust(route.getData());
                }
                break;
            //######任务失败######

            //方案1： 1.更新路由表中的数据状态为失败状态 2.考虑是否重试 hash算法 3.重试N次后，还是失败的，就 持久化任务
            //方案2： 1.更新路由表和持久层的状态为失败 2.删除路由记录 (失败的任务由上层决定是否要再次重试,并且决定重试的任务是否重新分配还是上次执行的节点)
            case TASK_FAIL:
                route.getData().setStatus(TaskState.FAIL.code());
                task.setStatus(TaskState.FAIL.code());
                if (storeApi.updateById(task)) {
                    //删除路由节点
                    router.removeRouteNode(clientId, taskId);
                }
                break;
        }
    }
}
