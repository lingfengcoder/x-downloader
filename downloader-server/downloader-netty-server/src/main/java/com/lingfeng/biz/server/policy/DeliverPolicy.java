package com.lingfeng.biz.server.policy;

import java.util.List;
import java.util.Map;

/**
 * @Author: wz
 * @Date: 2021/10/19 20:33
 * @Description: 任务分配策略接口
 */
public interface DeliverPolicy<I, T> {
    Map<I, List<T>> deliver(List<T> taskList, List<I> queueRemain);
}
