package com.pukka.iptv.downloader.mq.consumer;

import com.pukka.iptv.downloader.mq.model.MsgTask;
import com.pukka.iptv.downloader.mq.model.QueueChannel;
import com.pukka.iptv.downloader.mq.model.QueueInfo;
import com.pukka.iptv.downloader.mq.pool.MqUtil;
import com.pukka.iptv.downloader.mq.pool.RabbitMqPool;
import com.pukka.iptv.downloader.pool.Node;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.GetResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @Author: wz
 * @Date: 2021/10/13 16:27
 * @Description: mq 消息主动拉取的 消费者
 */
@Getter
@Setter
@Slf4j
@Accessors(fluent = true)
public class MqPullConsumer {
    private String name;
    private String id;
    //队列信息
    private QueueInfo mq;
    //消息的回调方法
    private ConsumerNotify work;
    //手动ack的回调方法
    private ConsumerAck ack;
    //是否自动ack
    private boolean autoAck = true;
    //是否自动归还channel
    private boolean autoBackChannel = true;

    private void makeInfo() {
        Thread thread = Thread.currentThread();
        this.id(thread.getId() + " " + thread.getName());
        // log.info("[MqPullConsumer id:{} name:{}] do work ", id, name);
    }

    private Node<RabbitMqPool.RKey, Channel> getMqChannel() {
        //从连接池中获取一个连接
        mq.routeKey(null).exchange(null);
        RabbitMqPool.RKey key = RabbitMqPool.RKey.newKey(mq);
        //采用不阻塞的方式获取
        return RabbitMqPool.me().pickNonBlock(key);
        //log.info("send work not get free channel,please wait for try");
    }

    //批量拉取 fetchCount条消息，没有就直接返回
    private boolean batchPull(Node<RabbitMqPool.RKey, Channel> node) {
        //开启监听
        Envelope envelope = null;
        Channel channel = null;
        try {
            if (node == null) return false;
            channel = node.getClient();
            GetResponse response = channel.basicGet(mq.queue(), false);
            if (response == null) {
                return false;
            }
            QueueChannel queueChannel = new QueueChannel(mq, channel, node);
            envelope = response.getEnvelope();
            MsgTask msgTask = new MsgTask();
            msgTask.setQueueChannel(queueChannel);
            msgTask.setDeliveryTag(envelope.getDeliveryTag());
            msgTask.setMsg(new String(response.getBody(), StandardCharsets.UTF_8));
            work.notify(msgTask);
        } catch (IOException e) {
            log.error("close channel num is {}", channel.getChannelNumber());
            log.error(e.getMessage(), e);
            return false;
        } finally {
            if (envelope != null) {
                try {
                    long deliveryTag = envelope.getDeliveryTag();
                    //是否是手动模式
                    if (ack != null) {
                        ack.doAck(channel, deliveryTag, null);
                    } else if (autoAck) {
                        //自动ack
                        MqUtil.ack(channel, deliveryTag);
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        return true;
    }

    public void pull() {
        //设置work信息
        makeInfo();
        Node<RabbitMqPool.RKey, Channel> node = null;
        boolean noMsg = true;
        try {
            int fetchCount = mq.fetchCount();
            node = getMqChannel();
            do {//批量拉取
                boolean b = batchPull(node);
                if (b) noMsg = false;
                fetchCount--;
            } while (fetchCount > 0);
        } catch (Exception e) {
            log.error("timeout key is {}", mq.queue());
            log.error(e.getMessage(), e);
        } finally {
            //如果拉取到了数据，当前节点就不能返还需要独占，
            // 防止其他线程抢占并关闭当前节点，造成ACK失败
            if (noMsg) {
                RabbitMqPool.me().back(node);
            } else {
                //拉取到消息的时候，需要看配置
                if (autoBackChannel) {
                    RabbitMqPool.me().back(node);
                }
            }
        }
    }


}
