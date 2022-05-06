package com.pukka.iptv.downloader.mq.consumer;


import com.rabbitmq.client.Channel;

/**
 * @Author: wz
 * @Date: 2021/10/15 10:50
 * @Description:
 */
@FunctionalInterface
public interface ConsumerAck {
    boolean doAck(Channel channel, long deliveryTag, String consumerTag);
}
