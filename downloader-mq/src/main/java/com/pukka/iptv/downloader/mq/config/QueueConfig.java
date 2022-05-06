package com.pukka.iptv.downloader.mq.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * @Author: wz
 * @Date: 2021/10/20 21:16
 * @Description: 队列配置
 */
@Order(1)
@Setter
@Getter
@ToString
@Configuration
@RefreshScope//动态刷新bean
public class QueueConfig {
    //任务队列和交换机
    @Value("${downloader.queue.taskQueue.exchange}")
    private String taskExchange;
    @Value("${downloader.queue.taskQueue.queue}")
    private String taskQueue;
    @Value("${downloader.queue.taskQueue.routingKey}")
    private String taskRoutingKey;

    //执行队列交换机
    @Value("${downloader.queue.executeQueue.exchange}")
    private String executeExchange;
    //执行队列前缀
    @Value("${downloader.queue.executeQueue.queuePrefix}")
    private String executeQueuePrefix;

    //重试队列
    @Value("${downloader.queue.retryQueue.exchange}")
    private String retryExchange;
    @Value("${downloader.queue.retryQueue.queue}")
    private String retryQueue;
    @Value("${downloader.queue.retryQueue.routingKey}")
    private String retryRoutingKey;

    //失败队列
    @Value("${downloader.queue.failedQueue.exchange}")
    private String failedExchange;
    @Value("${downloader.queue.failedQueue.queue}")
    private String failedQueue;
    @Value("${downloader.queue.failedQueue.routingKey}")
    private String failedRoutingKey;
}
