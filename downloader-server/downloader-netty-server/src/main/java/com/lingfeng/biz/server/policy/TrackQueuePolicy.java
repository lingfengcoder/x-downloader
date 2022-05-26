package com.lingfeng.biz.server.policy;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import com.lingfeng.biz.downloader.model.MsgTask;
import com.lingfeng.biz.downloader.model.QueueInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @Author: wz
 * @Date: 2021/10/19 20:45
 * @Description: 追踪队列算法 根据上次处理消息的节点信息 再次发送时任然发送处理的节点中
 */
@Slf4j
@Component
public class TrackQueuePolicy implements RoutePolicy<QueueInfo, MsgTask> {

    //简单策略模式
    //1.先感知存活的 执行(消费)队列的长度 2.将任务合理分配给 每个执行队列 剩余空位 3.发送消息
    @Override
    public Map<QueueInfo, List<MsgTask>> deliver(List<MsgTask> taskList, List<QueueInfo> queueRemain) {
        //没有剩余不执行
        if (ObjectUtil.isEmpty(queueRemain) || ObjectUtil.isEmpty(taskList)) return null;
        //打印空闲队列和数量
        for (QueueInfo queue : queueRemain) {
            log.info(queue.queue() + "空闲:{}", queue.remainCount());
        }
        //追踪节点式分配任务
        return trackDeliveryTask(taskList, queueRemain);
    }

    //设置为重试信息
    private void setRetryFlag(MsgTask task) {
        task.setRetryMsg(true);
    }

    //获取上次处理该消息的节点id的 routekey(用于发送消息到指定队列)
    private String getLastConsumerRouteKey(MsgTask task) {
        String msg = task.getMsg();
        JSONObject json = JSONObject.parseObject(msg);
        return json.getString("taskServerInstanceId");
    }

    //对任务进行分组 key:routeKey value:任务集合
    private Map<String, List<MsgTask>> preparedTaskGroup(List<MsgTask> taskList) {
        Map<String, List<MsgTask>> result = new HashMap<>();
        Iterator<MsgTask> iterator = taskList.iterator();
        while (iterator.hasNext()) {
            MsgTask next = iterator.next();
            //设置为 重试消息 的标记
            setRetryFlag(next);
            //获取上一次处理该消息的队列routeKey
            String routeKey = getLastConsumerRouteKey(next);
            if (routeKey != null) {
                List<MsgTask> msgTasks = result.computeIfAbsent(routeKey, k -> new ArrayList<>());
                msgTasks.add(next);
            } else {
                //删除不存在routeKey的数据，避免消息积压，需要用日志记录，并记录在日志中
                log.error("msgTask 存在于重试队列但routeKey为空 {}", next);
                //QueueChannel queueChannel = next.getQueueChannel();
                //note MqUtil.ack(queueChannel.channel(), next.getDeliveryTag());
            }
        }
        return result;
    }

    //追踪链路式分配数据
    //根据任务上次分配的队列信息，将任务分配到上一次执行的节点上，
    private Map<QueueInfo, List<MsgTask>> trackDeliveryTask(List<MsgTask> taskList, List<QueueInfo> queues) {
        //结果存放集合
        Map<QueueInfo, List<MsgTask>> result = new HashMap<>();
        //根据routeKey对消息进行分组
        Map<String, List<MsgTask>> msgGroup = preparedTaskGroup(taskList);
        for (QueueInfo queue : queues) {
            //获取当前routeKey下的所有任务
            List<MsgTask> msgTasks = msgGroup.get(queue.routeKey());
            if (ObjectUtil.isEmpty(msgTasks)) continue;
            //根据队列的 重试剩余空位进行充分分配
            int remainRetryCount = queue.retryCount();
            if (remainRetryCount > 0) {
                //从任务中拿取N个任务
                List<MsgTask> job = getNTaskFromTaskList(msgTasks, remainRetryCount);
                //对需要发送的进行删除
                for (MsgTask task : job) {
                    int i = taskList.indexOf(task);
                    if (i != -1) taskList.remove(i);
                }
                result.put(queue, job);
            }
        }
        return result;
    }


    /**
     * @Description: 从任务中取出 N个任务
     * @param: [n:需要取出的任务个数, taskList:任务集合]
     * @return: java.util.List<com.lingfeng.biz.downloader.mq.entity.MsgTask>
     * @author: wz
     * @date: 2021/10/19 20:54
     */
    private static List<MsgTask> getNTaskFromTaskList(List<MsgTask> taskList, int count) {
        Iterator<MsgTask> iterator = taskList.iterator();
        List<MsgTask> result = new ArrayList<>(count);
        while (iterator.hasNext()) {
            if (count == 0) break;
            result.add(iterator.next());
            iterator.remove();
            count--;
        }
        return result;
    }

}
