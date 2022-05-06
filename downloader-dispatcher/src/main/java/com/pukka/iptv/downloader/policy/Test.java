package com.pukka.iptv.downloader.policy;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.pukka.iptv.downloader.mq.model.MsgTask;
import com.pukka.iptv.downloader.mq.model.QueueInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Author: wz
 * @Date: 2022/2/15 14:01
 * @Description:
 */
public class Test {


    public static void main(String[] args) throws CloneNotSupportedException {
        //总任务数
        int maxTask = 10000;
        //队列数
        int maxQueueLen = 500;
        //每个队列最多任务数
        int perMaxTask = 50;
        TestData data = general(perMaxTask, maxTask, maxQueueLen);

        WorkMoreGetMorePlusPolicy workMoreGetMorePlusPolicy = new WorkMoreGetMorePlusPolicy();
        WorkMoreGetMorePolicy workMoreGetMorePolicy = new WorkMoreGetMorePolicy();

        test(workMoreGetMorePlusPolicy, data);

        test(workMoreGetMorePolicy, data);

    }

    private static void test(DeliverPolicy<QueueInfo, MsgTask> policy, TestData data)  {
        TestData testData = new TestData();
        BeanUtil.copyProperties(data, testData);
        long start = System.currentTimeMillis();
        Map<QueueInfo, List<MsgTask>> deliver = policy.deliver(testData.getRemainTask(), testData.getQueues());

        long end = System.currentTimeMillis();
        System.out.println(policy.getClass().getName() + " 耗时:" + (end - start));
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    static
    class TestData implements Cloneable {
        int perMaxTask;
        int maxTask;
        int maxQueueLen;
        List<MsgTask> remainTask;
        List<QueueInfo> queues;
    }

    private static TestData general(int perMaxTask, int maxTask, int maxQueueLen) {
        List<MsgTask> remainTask = new ArrayList<>(maxTask);
        for (int i = 0; i < maxTask; i++) {
            remainTask.add(new MsgTask().setMsg("msg: " + i));
        }
        List<QueueInfo> queues = new ArrayList<>(maxQueueLen);
        //随机饥饿
        for (int i = 0; i < maxQueueLen; i++) {
            int random = RandomUtil.randomInt(perMaxTask);
            queues.add(new QueueInfo().queue("q" + i).remainCount(random).queueLen(perMaxTask - random));
        }
        return new TestData().setMaxQueueLen(maxQueueLen)
                .setMaxTask(maxTask)
                .setPerMaxTask(perMaxTask)
                .setRemainTask(remainTask)
                .setQueues(queues);
    }
}
