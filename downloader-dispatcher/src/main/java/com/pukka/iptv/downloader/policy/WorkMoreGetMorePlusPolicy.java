package com.pukka.iptv.downloader.policy;

import cn.hutool.core.util.ObjectUtil;
import com.pukka.iptv.downloader.mq.model.MsgTask;
import com.pukka.iptv.downloader.mq.model.QueueInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;


/**
 * @Author: wz
 * @Date: 2021/10/19 20:45
 * @Description: "多劳多得 少劳少得"算法 合理均衡各个节点任务
 */
@Slf4j
@Component
public class WorkMoreGetMorePlusPolicy implements DeliverPolicy<QueueInfo, MsgTask> {


    //简单策略模式
    //1.先感知存活的 执行(消费)队列的长度 2.将任务合理分配给 每个执行队列 剩余空位 3.发送消息
    @Override
    public Map<QueueInfo, List<MsgTask>> deliver(List<MsgTask> taskList, List<QueueInfo> queueRemain) {
        //没有剩余不执行
        if (ObjectUtil.isEmpty(queueRemain) || ObjectUtil.isEmpty(taskList)) return null;
        //打印空闲队列和数量
        for (QueueInfo queue : queueRemain) {
            //  log.info(queue.queue() + "空闲:{}", queue.remainCount());
        }
        //平均分配任务
        Map<QueueInfo, List<MsgTask>> result = workMoreGetMoreDeliveryTask(taskList, queueRemain);
        if (!taskList.isEmpty()) {
            log.info("队列都已经满,但还有{}个任务没有分配", taskList.size());
        }
        return result;
    }


    /**
     * @return void
     * @Description 通过数学权重方法进行分配
     * @author wz
     * @date 2022/2/15 10:13
     */
    private static Map<QueueInfo, List<MsgTask>> workMoreGetMoreDeliveryTask(List<MsgTask> taskList, List<QueueInfo> queueRemain) {
        int totalFree = 0;
        for (QueueInfo info : queueRemain) {
            totalFree += info.remainCount();
        }
        //如果任务数大于等于空闲数,直接全部分配
        if (taskList.size() >= totalFree) {
            return directlyDeliver(taskList, queueRemain);
        } else {
            return quanzhong(taskList, queueRemain);
        }
    }

    //直接分配法
    private static Map<QueueInfo, List<MsgTask>> directlyDeliver(List<MsgTask> taskList, List<QueueInfo> queueRemain) {
        Map<QueueInfo, List<MsgTask>> result = new HashMap<>();
        for (QueueInfo info : queueRemain) {
            result.put(info, takeTask(taskList, info.remainCount()));
        }
        return result;
    }

    //权重分配算法
    private static Map<QueueInfo, List<MsgTask>> quanzhong(List<MsgTask> taskList, List<QueueInfo> queueRemain) {
        int base = taskList.size();
        //计算权重
        Map<QueueInfo, Integer> queueWithTaskCount = new HashMap<>();
        //通过丢弃大于平均值
        deliverWithDropBigger(base, queueRemain, queueWithTaskCount);
        Map<QueueInfo, List<MsgTask>> result = new HashMap<>();
        for (Map.Entry<QueueInfo, Integer> item : queueWithTaskCount.entrySet()) {
            Integer value = item.getValue();
            QueueInfo info = item.getKey();
            //从剩余任务中获取
            List<MsgTask> task = takeTask(taskList, Math.min(value, info.remainCount()));
            //更新队列剩余个数
            info.remainCount(info.remainCount() - task.size());
            result.put(info, task);
        }
        return result;
    }

    private static void deliverWithDropBigger(int base, List<QueueInfo> queueRemain, Map<QueueInfo, Integer> result) {
        if (CollectionUtils.isEmpty(queueRemain) || base <= 0) {
            return;//递归跳出点
        }
        //计算总量
        int all = queueRemain.stream().mapToInt(QueueInfo::queueLen).sum() + base;
        //大于平均值的集合
        List<QueueInfo> overAvgList = new ArrayList<>(0);
        long avg = Math.round((all + 0.0) / queueRemain.size());
        for (QueueInfo info : queueRemain) {
            if (info.queueLen() > avg) {
                overAvgList.add(info);
            }
        }
        //将大于平均值的去除
        if (overAvgList.size() > 0) {
            for (QueueInfo idx : overAvgList) {
                queueRemain.remove(idx);
            }
            //超过平均量的不参与分配，将其排除后再次进行递归分配
            deliverWithDropBigger(base, queueRemain, result);
        } else {
            //分配
            for (QueueInfo info : queueRemain) {
                result.put(info, Math.round(avg - info.queueLen()));
            }
        }
    }


    /**
     * @Description: 从任务中取出 N个任务
     * @param: [n:需要取出的任务个数, taskList:任务集合]
     * @return: java.util.List<com.pukka.iptv.downloader.mq.entity.MsgTask>
     * @author: wz
     * @date: 2021/10/19 20:54
     */
    private static List<MsgTask> takeTask(List<MsgTask> taskList, int count) {
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
