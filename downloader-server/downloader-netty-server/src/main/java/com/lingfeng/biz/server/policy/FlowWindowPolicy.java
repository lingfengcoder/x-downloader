package com.lingfeng.biz.server.policy;


import com.lingfeng.biz.downloader.model.MsgTask;
import com.lingfeng.biz.downloader.model.QueueInfo;

import java.util.List;
import java.util.Map;

/**
 * @Author: wz
 * @Date: 2021/11/24 14:11
 * @Description: 流量窗口算法
 */
public class FlowWindowPolicy implements RoutePolicy<QueueInfo, MsgTask> {
    @Override
    public Map<QueueInfo, List<MsgTask>> deliver(List<MsgTask> taskList, List<QueueInfo> queueRemain) {
        return null;
    }
}
