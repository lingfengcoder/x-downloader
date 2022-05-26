package com.lingfeng.biz.server.policy;

import cn.hutool.core.util.ObjectUtil;

import com.lingfeng.biz.downloader.model.DownloadTask;
import com.lingfeng.biz.downloader.model.NodeRemain;
import com.lingfeng.biz.downloader.util.ListUtils;
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
public class WorkMoreGetMorePlusPolicy implements RoutePolicy<NodeRemain, DownloadTask> {


    //简单策略模式
    //1.先感知存活的 执行(消费)队列的长度 2.将任务合理分配给 每个执行队列 剩余空位 3.发送消息
    @Override
    public Map<NodeRemain, List<DownloadTask>> deliver(List<DownloadTask> taskList, List<NodeRemain> queueRemain) {
        //没有剩余不执行
        if (ObjectUtil.isEmpty(queueRemain) || ObjectUtil.isEmpty(taskList)) return null;
        //打印空闲队列和数量
        //平均分配任务
        Map<NodeRemain, List<DownloadTask>> result = workMoreGetMoreDeliveryTask(taskList, queueRemain);
        if (!taskList.isEmpty()) {
            log.info("队列都已经满,但还有{}个任务没有分配", taskList.size());
        }
        return result;
    }


    /**
     * @return void
     * @Description 通过数学权重方法进行分配
     * @author wz
     * @date 2022/2/15 10:13 <NodeRemain, Integer>
     */
    private static Map<NodeRemain, List<DownloadTask>> workMoreGetMoreDeliveryTask(List<DownloadTask> taskList, List<NodeRemain> queueRemain) {
        int totalFree = 0;
        for (NodeRemain node : queueRemain) {
            totalFree += node.getRemain();
        }
        //如果任务数大于等于空闲数,直接全部分配
        if (taskList.size() >= totalFree) {
            return directlyDeliver(taskList, queueRemain);
        } else {
            return weightDeliver(taskList, queueRemain);
        }
    }

    //直接分配法
    private static Map<NodeRemain, List<DownloadTask>> directlyDeliver(List<DownloadTask> taskList, List<NodeRemain> queueRemain) {
        Map<NodeRemain, List<DownloadTask>> result = new HashMap<>();
        for (NodeRemain info : queueRemain) {
            result.put(info, ListUtils.subList(taskList, info.getRemain()));
        }
        return result;
    }

    //如果任务不够分配，则进行权重算法
    private static Map<NodeRemain, List<DownloadTask>> weightDeliver(List<DownloadTask> taskList, List<NodeRemain> queueRemain) {
        int base = taskList.size();
        //计算权重
        Map<NodeRemain, Integer> queueWithTaskCount = new HashMap<>();
        //通过丢弃大于平均值
        deliverWithDropBigger(base, queueRemain, queueWithTaskCount);
        Map<NodeRemain, List<DownloadTask>> result = new HashMap<>();
        for (Map.Entry<NodeRemain, Integer> item : queueWithTaskCount.entrySet()) {
            Integer value = item.getValue();
            NodeRemain info = item.getKey();
            //从剩余任务中获取
            List<DownloadTask> task = ListUtils.subList(taskList, Math.min(value, info.getRemain()));
            //更新队列剩余个数
            info.setRemain(info.getRemain() - task.size());
            result.put(info, task);
        }
        return result;
    }

    private static void deliverWithDropBigger(int base, List<NodeRemain> queueRemain, Map<NodeRemain, Integer> result) {
        if (CollectionUtils.isEmpty(queueRemain) || base <= 0) {
            return;//递归跳出点
        }
        //计算总量
        int all = queueRemain.stream().mapToInt(NodeRemain::getRemain).sum() + base;
        //大于平均值的集合
        List<NodeRemain> overAvgList = new ArrayList<>(0);
        long avg = Math.round((all + 0.0) / queueRemain.size());
        for (NodeRemain info : queueRemain) {
            if (info.getRemain() > avg) {
                overAvgList.add(info);
            }
        }
        //将大于平均值的去除
        if (overAvgList.size() > 0) {
            for (NodeRemain idx : overAvgList) {
                queueRemain.remove(idx);
            }
            //排除超过平均量的，再次进行递归分配
            deliverWithDropBigger(base, queueRemain, result);
        } else {
            //分配
            for (NodeRemain info : queueRemain) {
                result.put(info, Math.round(avg - info.getMax()));
            }
        }
    }


}
