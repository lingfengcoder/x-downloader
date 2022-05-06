package com.pukka.iptv.downloader.policy;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.SystemClock;
import cn.hutool.core.util.ObjectUtil;
import com.pukka.iptv.downloader.mq.model.MsgTask;
import com.pukka.iptv.downloader.mq.model.QueueInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: wz
 * @Date: 2021/10/19 20:45
 * @Description: "多劳多得 少劳少得"算法 合理均衡各个节点任务
 */
@Slf4j
@Component
public class WorkMoreGetMorePolicy implements DeliverPolicy<QueueInfo, MsgTask> {
    //上一次发布消息的优先级
    private static volatile Map<QueueInfo, Integer> lastQueuePriority = new HashMap<>();
    //上一次同步优先级的时间
    private static volatile long lastPriorityBalanceTime = 0L;

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
        //平均分配任务
        return workMoreGetMoreDeliveryTask(taskList, queueRemain);
    }

    /**
     * @Description: 任务派送 获取每个队列剩余的个数 尽可能进行均匀分配
     * @param: [taskList:待分配的任务, queue:需要分配任务的队列] queues是 空闲数从小到大排序的
     * @Author: wz
     * @date: 2021/10/15 18:08
     */
    private Map<QueueInfo, List<MsgTask>> workMoreGetMoreDeliveryTask(List<MsgTask> taskList, List<QueueInfo> queues) {
        Map<QueueInfo, List<MsgTask>> result = new HashMap<>();
        workMoreGetMore(queues, taskList, result);
        clearLastPriority(false);
        //如果分配完毕还有剩余，此时所有队列都已经满了
        if (!taskList.isEmpty()) {
            log.info("队列都已经满,但还有{}个任务没有分配", taskList.size());
        }
        return result;
    }


    //获取还有空闲位置的队列个数
    private static List<QueueInfo> getNotFullQueue(List<QueueInfo> queues) {
        if (!ObjectUtil.isEmpty(queues)) {
            return queues.stream().filter(i -> i.remainCount() > 0).collect(Collectors.toList());
        }
        return null;
    }

    /**
     * @Description: 采用递归均分任务 递归跳出条件: 1.任务全部分配完毕 2.没有所有队列都已满
     * @param: [queues:需要待分配任务的队列, remainTask:待分配的任务集合, result:分配结果]
     * @return: java.util.Map<QueueInfo, List < MsgTask>>
     * @author: wz
     * @date: 2021/10/19 20:51
     */
    private static void workMoreGetMore(List<QueueInfo> queues, List<MsgTask> remainTask,
                                        Map<QueueInfo, List<MsgTask>> result) {
        //获取有空闲的队列
        List<QueueInfo> notFullQueue = getNotFullQueue(queues);
        //没有空闲的队列就直接返回
        if (ObjectUtil.isEmpty(notFullQueue)) return;
        //统计 待分配任务的 队列个数
        int remainSize = notFullQueue.size();
        //如果没有空闲队列则返回
        if (remainSize == 0) return;
        //每个队列 应该分到的个数
        int n = remainTask.size() / remainSize;
        //剩余数量
        int leave = remainTask.size() % remainSize;
        //如果剩余不够分，直接进行分配
        n = n == 0 ? leave : n;
        //每个队列从 剩余任务中获取 指定个数（n）的任务
        //排序 将空闲多的放前面 尽量均匀分配
        CollectionUtil.sort(notFullQueue, (a, b) -> {
            int order = b.remainCount() - a.remainCount();
            if (order == 0) {//如果剩余个数相同，则优先未分配的队列
                order = comparePriority(a, b);
            }
            return order;
        });
        //均衡 避免单一队列分配过，造成分配不均匀
        if (notFullQueue.size() > 1) {
            QueueInfo first = notFullQueue.get(0);
            QueueInfo second = notFullQueue.get(1);
            int diff = first.remainCount() - second.remainCount();
            diff = diff == 0 ? 1 : diff;
            n = Math.min(n, diff);
        }
        {
            QueueInfo queue = notFullQueue.get(0);
            //更新队列优先级
            updateQueuePriority(queue);
            //队列剩余数
            int c = queue.remainCount();
            //剩余能把平均的都装下,装N个
            c = Math.min(c, n);//最多装N个，否则能装几个就装几个
            //从剩余任务中拿取c个
            List<MsgTask> tmp = getNTaskFromTaskList(remainTask, c);
            //更新队列剩余空闲数
            queue.remainCount(queue.remainCount() - tmp.size());
            //装任务
            putRemainTask(queue, tmp, result);
            //跳出递归条件1: 如果分完了就停止
            if (remainTask.size() == 0)
                return;
        }
        //如果还有空余队列,则递归投递
        workMoreGetMore(queues, remainTask, result);
    }

    //如果超时 清空上一次优先级记录
    private static void clearLastPriority(boolean focus) {
        if (focus) {
            lastQueuePriority.clear();
            //上一次的记录超时之后进行清除
        } else if (lastPriorityBalanceTime != 0 && SystemClock.now() - lastPriorityBalanceTime > 10000) {
            lastQueuePriority.clear();
        }
    }

    //根据优先级比较 优先分配次数少的进行分配  从小到大排序
    private static Integer comparePriority(QueueInfo a, QueueInfo b) {
        //设置本次更新时间
        if (!lastQueuePriority.containsKey(a) && !lastQueuePriority.containsKey(b)) {
            return 0;
        } else if (!lastQueuePriority.containsKey(b)) {
            return 1;
        } else if (!lastQueuePriority.containsKey(a)) {
            return -1;
        }
        return lastQueuePriority.get(b) - lastQueuePriority.get(a);
    }

    //更新队列优先级
    private static void updateQueuePriority(QueueInfo info) {
        boolean exist = lastQueuePriority.containsKey(info);
        if (exist) {
            Integer priority = lastQueuePriority.get(info);
            lastQueuePriority.put(info, ++priority);
        } else {
            lastQueuePriority.put(info, 1);
        }
    }

    public static void main(String[] args) {

        int x = 10;
        List<MsgTask> remainTask = new ArrayList<>();
        for (int i = 0; i < x; i++) {
            remainTask.add(new MsgTask().setMsg("msg: " + x));
        }


        List<QueueInfo> queues = new ArrayList<>();
        int c = 4;

        queues.add(new QueueInfo().queue("q1").remainCount(2));
        queues.add(new QueueInfo().queue("q2").remainCount(2));
        queues.add(new QueueInfo().queue("q3").remainCount(3));
        queues.add(new QueueInfo().queue("q4").remainCount(3));
        queues.add(new QueueInfo().queue("q5").remainCount(6));
        queues.add(new QueueInfo().queue("q6").remainCount(6));

        Map<QueueInfo, List<MsgTask>> result = new HashMap<>();
        workMoreGetMore(queues, remainTask, result);

        System.out.println(queues);
    }


    /**
     * @Description: 添加指定任务到队列中
     * @param: [queue:队列信息, data:需要添加的数据, result:结果]
     * @return: void
     * @author: wz
     * @date: 2021/10/19 20:49
     */
    private static void putRemainTask(QueueInfo queue, List<MsgTask> data,
                                      Map<QueueInfo, List<MsgTask>> result) {
        List<MsgTask> fileTasks = result.get(queue);
        if (fileTasks == null) {
            result.put(queue, data);
        } else {
            fileTasks.addAll(data);
        }
    }

    /**
     * @Description: 从任务中取出 N个任务
     * @param: [n:需要取出的任务个数, taskList:任务集合]
     * @return: java.util.List<com.pukka.iptv.downloader.mq.entity.MsgTask>
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
