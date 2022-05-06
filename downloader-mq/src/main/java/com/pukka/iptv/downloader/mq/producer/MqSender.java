package com.pukka.iptv.downloader.mq.producer;

import com.alibaba.fastjson.JSONObject;

import com.pukka.iptv.downloader.mq.model.QueueInfo;
import com.pukka.iptv.downloader.mq.pool.RabbitMqPool;
import com.pukka.iptv.downloader.pool.Node;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConfirmListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

/**
 * @Author: wz
 * @Date: 2021/10/15 10:38
 * @Description:
 */
@Slf4j
public class MqSender {
    //默认发送超时时间
    private final static long DEFAULT_SEND_TIMEOUT = 5000;

    //阻塞式发送消息
    public static void sendMsgWaitFor(String msg, QueueInfo info, long timeout, SendWaitCallBack waitCallBack) throws Exception {
        info.queue(null);
        Node<RabbitMqPool.RKey, Channel> node = getChannel(info);
        if (!StringUtils.hasLength(info.routeKey())) info.routeKey("");
        if (node == null) throw new RuntimeException("获取channel失败！请重新发送消息");
        Channel channel = node.getClient();
        try {
            // log.info("发送消息:{} 到 队列:{}", msg, info);
            String exchange = info.exchange();
            AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder();
            AMQP.BasicProperties build = builder.contentType("application/json").build();
            // AMQP.BasicProperties build = builder.contentType(MessageProperties.CONTENT_TYPE_TEXT_PLAIN).build();
            //开启发送确认机制
            channel.confirmSelect();
            channel.basicPublish(exchange, info.routeKey(), true, build, msg.getBytes(StandardCharsets.UTF_8));
            boolean b = channel.waitForConfirms(timeout);
        } finally {
            try {
                if (waitCallBack != null)
                    waitCallBack.callback();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            //归还
            log.info("sendMsgWaitFor back =>{}", node.toString());
            RabbitMqPool.me().back(node);
        }
    }

    //阻塞式发送消息
    public static void sendMsgWaitFor(String msg, QueueInfo info, SendWaitCallBack waitCallBack) throws Exception {
        sendMsgWaitFor(msg, info, DEFAULT_SEND_TIMEOUT, waitCallBack);
    }

    /**
     * @Description: 发送mq消息
     * @param: [msg, info]
     * @return: void
     * @Author: wz
     * @date: 2021/10/15 10:47
     */
    public static void sendMsg(String msg, QueueInfo info, SendCallBack sendCallBack) {
        if (!StringUtils.hasLength(info.routeKey())) info.routeKey("");
        info.queue(null);
        Node<RabbitMqPool.RKey, Channel> node = getChannel(info);
        if (node == null) throw new RuntimeException("获取channel失败！请重新发送消息");
        Channel channel = node.getClient();
        try {
            String exchange = info.exchange();
            AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder();
            AMQP.BasicProperties build = builder.contentType("application/json").build();
            // AMQP.BasicProperties build = builder.contentType(MessageProperties.CONTENT_TYPE_TEXT_PLAIN).build();
            //开启发送确认机制
            if (sendCallBack != null) {
                channel.confirmSelect();
                channel.addConfirmListener(new ConfirmListener() {
                    @Override
                    public void handleAck(long deliveryTag, boolean multiple) {
                        log.info("deliveryTag: {}", deliveryTag);
                        sendCallBack.sendSuccess(deliveryTag);
                    }

                    @Override
                    public void handleNack(long deliveryTag, boolean multiple) {
                        log.error("消息发送失败: {}", deliveryTag);
                        sendCallBack.sendFail(deliveryTag);
                    }
                });
            }
            channel.basicPublish(exchange, info.routeKey(), true, build, msg.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            //归还
            //log.info("sendMsgWaitFor back =>{}", node.toString());
            RabbitMqPool.me().back(node);
        }
    }

    public static void sendMsg(Object msg, QueueInfo info, SendCallBack sendCallBack) {
        sendMsg(JSONObject.toJSONString(msg), info, sendCallBack);
    }

    public static void sendMsg(Object msg, QueueInfo info) {
        sendMsg(JSONObject.toJSONString(msg), info, null);
    }

    //采用不阻塞超时的方式获取
    private static Node<RabbitMqPool.RKey, Channel> getChannel(QueueInfo info) {
        RabbitMqPool.RKey key = RabbitMqPool.RKey.newKey(info);
        //采用不阻塞超时的方式获取
        Node<RabbitMqPool.RKey, Channel> node = RabbitMqPool.me().pickBlock(key, 5000);
        if (node == null) {
            log.info("send work not get free channel,please wait for try");
            return null;
        }
        return node;
    }
}
