package com.pukka.iptv.downloader.mq.pool;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson.JSONObject;
import com.pukka.iptv.downloader.mq.config.RabbitMqConfig;
import com.pukka.iptv.downloader.mq.config.RestHttp;
import com.pukka.iptv.downloader.mq.model.QueueInfo;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * @Author: wz
 * @Date: 2021/10/19 16:26
 * @Description: rabbitmq工具类，有声明队列、关闭channel、ACK、NACK、查询队列信息等功能
 */
@Slf4j
public class MqUtil {

    /**
     * @Description: 自动声明或者绑定 交换机、队列 信息
     * @param: [info]
     * @return: boolean
     * @author: wz
     * @date: 2021/10/28 17:31
     */
    public static boolean declareOrBindQueueAndExchange(Channel channel, QueueInfo info) {
        try {
            if (!assertChannel(channel)) return false;
            String queue = info.queue();
            String exchange = info.exchange();
            String routeKey = info.routeKey();
            String type = info.type();
            if (ObjectUtil.isEmpty(queue) || ObjectUtil.isEmpty(exchange)) {
                throw new RuntimeException("队列绑定交换机 不能为空 队列或者交换机不能为空!");
            }
            if ("direct".equals(type)) {
                if (ObjectUtil.isEmpty(routeKey)) {
                    throw new RuntimeException("direct 模式下 路由key 不能为空");
                }
            }
            //声明交换机
            channel.exchangeDeclare(exchange, type, true, false, null);
            //声明队列
            channel.queueDeclare(queue, true, false, false, null);
            //管道绑定
            channel.queueBind(queue, exchange, routeKey);
            return true;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }


    //ack 应答
    public static boolean ack(Channel channel, Long deliveryTag) {
        try {
            if (!assertChannel(channel)) return false;
            log.info("channel={} deliveryTag={}执行ack", channel.getChannelNumber(), deliveryTag);
            if (channel.isOpen()) {
                channel.basicAck(deliveryTag, false);
            } else {
                log.error("ack channel is not open!!!");
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
        return true;
    }

    //nack 应答
    public static boolean nack(Channel channel, Long deliveryTag) {
        try {
            Thread.currentThread().interrupt();
            if (!assertChannel(channel)) return false;
            if (channel.isOpen()) {
                channel.basicNack(deliveryTag, false, true);
            } else {
                log.error("nack channel is not open");
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
        return true;
    }

    //关闭连接
    public static boolean closeChannel(Channel channel) {
        return closeChannel(channel, false);
    }

    //关闭通道
    public static boolean closeChannel(Channel channel, boolean closeConnect) {
        try {
            if (!assertChannel(channel)) return false;
            //关闭channel连接
            if (channel.isOpen()) {
                channel.close();
                if (closeConnect)
                    channel.getConnection().close();
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return false;
        }
        return true;
    }

    public static boolean clearBlockedListeners(Channel channel) {
        if (!assertChannel(channel)) return false;
        Connection connection = channel.getConnection();
        if (connection != null) {
            connection.clearBlockedListeners();
            return true;
        }
        return false;
    }

    //取消监听
    public boolean unSubscribeChannel(Channel channel, String consumerTag) {
        try {
            if (!assertChannel(channel)) return false;
            channel.basicCancel(consumerTag);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return false;
        }
        return true;
    }

    private static boolean assertChannel(Channel channel) {
        if (channel == null) {
            log.warn("channel == null !");
            return false;
        }
        return true;
    }

    //通过mq的15672端口获取队列信息数据
    // 这个接口数据并不是实时的,不能精准获取mq队列大小
    // 测试中发现存在大概1S的延时(通过发送mq消息并确认送达后,此时任然会存在时间差)
    public static Integer getQueueLen(String queueName) {
        RabbitMqConfig bean = SpringUtil.getBean(RabbitMqConfig.class);
        String url = "http://" + bean.getAddresses().split(":")[0] + ":15672/api/queues/" + bean.getVhost() + "/" + queueName;
        try {
            JSONObject data = RestHttp.httpAuth(url, bean.getUsername(), bean.getPassword());
            int len = -1;
            len = Math.max(len, data.getInteger("messages_ram"));
            len = Math.max(len, data.getInteger("messages_ready_ram"));
            len = Math.max(len, data.getInteger("messages_ready"));
            len = Math.max(len, data.getInteger("messages"));
            //message 队列长度
            return len;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return -1;
    }
}
