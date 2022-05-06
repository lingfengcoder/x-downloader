package com.pukka.iptv.downloader.mq.model;

import com.pukka.iptv.downloader.mq.pool.RabbitMqPool;
import com.pukka.iptv.downloader.pool.Node;
import com.rabbitmq.client.Channel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * @Author: wz
 * @Date: 2021/10/13 16:54
 * @Description:
 */
@Setter
@Getter
@Slf4j
@Accessors(fluent = true)
public class QueueChannel {
    //队列信息
    private QueueInfo queue;
    //管道
    private Channel channel;
    //连接节点
    private Node<RabbitMqPool.RKey, Channel> node;

    public QueueChannel(QueueInfo queue, Channel channel, Node<RabbitMqPool.RKey, Channel> node) {
        this.queue = queue;
        this.channel = channel;
        this.node = node;
    }

    //下面两个方法不可删除
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueueChannel queueChannel = (QueueChannel) o;
        return queue.equals(queueChannel.queue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(queue);
    }
}
