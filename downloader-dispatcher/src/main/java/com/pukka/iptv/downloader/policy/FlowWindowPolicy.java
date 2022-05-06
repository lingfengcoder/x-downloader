package com.pukka.iptv.downloader.policy;

import com.pukka.iptv.downloader.mq.model.MsgTask;
import com.pukka.iptv.downloader.mq.model.QueueInfo;

import java.util.List;
import java.util.Map;

/**
 * @Author: wz
 * @Date: 2021/11/24 14:11
 * @Description: 流量窗口算法
 */
public class FlowWindowPolicy implements DeliverPolicy<QueueInfo, MsgTask> {
    @Override
    public Map<QueueInfo, List<MsgTask>> deliver(List<MsgTask> taskList, List<QueueInfo> queueRemain) {
        return null;
    }
}
