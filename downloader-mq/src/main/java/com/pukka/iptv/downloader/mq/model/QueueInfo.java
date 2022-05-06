package com.pukka.iptv.downloader.mq.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Objects;

/**
 * @Author: wz
 * @Date: 2021/10/13 18:01
 * @Description:
 */
@Getter
@Setter
@ToString
@Accessors(fluent = true)
public class QueueInfo {
    //交换机
    private String exchange;
    //队列名
    private String queue;
    //路由key
    private String routeKey;
    //交换机类型
    private String type = "direct";
    //默认拉取的数据条数
    private int fetchCount = 1;
    //队列实际长度
    private int queueLen;
    //队列剩余空闲数
    private int remainCount;
    //重试队列剩余空闲数
    private int retryCount;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueueInfo queueInfo = (QueueInfo) o;
        return Objects.equals(exchange, queueInfo.exchange) &&
                Objects.equals(queue, queueInfo.queue) &&
                Objects.equals(routeKey, queueInfo.routeKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(exchange, queue, routeKey);
    }
}
