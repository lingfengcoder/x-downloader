package com.pukka.iptv.downloader.mq.consumer;

import cn.hutool.extra.spring.SpringUtil;
import com.pukka.iptv.downloader.mq.model.MsgTask;
import com.pukka.iptv.downloader.mq.model.QueueChannel;
import com.pukka.iptv.downloader.mq.model.QueueInfo;
import com.pukka.iptv.downloader.mq.pool.MqConnection;
import com.pukka.iptv.downloader.mq.pool.MqUtil;
import com.pukka.iptv.downloader.mq.pool.RabbitMqPool;
import com.pukka.iptv.downloader.pool.Node;
import com.rabbitmq.client.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @Author: wz
 * @Date: 2021/10/13 16:27
 * @Description: mq 监听模式消息消费者
 */
@Getter
@Setter
@Slf4j
@Accessors(fluent = true)
public class MqConsumer {
    private String name;
    private String id;
    private QueueInfo mq;
    private Channel channel;
    private ConsumerNotify work;
    private ConsumerAck ack;
    //是否是关闭状态
    private volatile boolean closed = false;
    //是否自动ack
    private boolean autoAck = true;

    public void setCloseFlag() {
        this.closed = true;
    }

    public boolean isClosed() {
        return this.closed;
    }


    private void makeInfo() {
        Thread thread = Thread.currentThread();
        this.id(thread.getId() + " " + thread.getName());
        //log.info("[MqConsumer   id:{} name:{}] do work ", id, name);
    }

    private Channel getMqChannel() {
        try {
            MqConnection mqBean = SpringUtil.getBean(MqConnection.class);
            Connection connection = mqBean.getConnection();
            return connection.createChannel();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public boolean listen() {
        //设置work信息
        makeInfo();
        try {
            Channel channel = getMqChannel();
            if (channel == null) return false;
            this.channel = channel;
            //每次只拉取一条，用于配合连接池线程实现，动态调配
            channel.basicQos(0, 10, false);
            String s = channel.basicConsume(mq.queue(), false, new Consumer() {
                @Override
                //处理信息
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
                    try {
                        //创建队列和管道的绑定信息
                        QueueChannel queueChannel = new QueueChannel(mq, channel, null);
                        //Thread thread = Thread.currentThread();
                        //log.warn("[handleDelivery] consumer info my thread is {}", thread.getId() + thread.getName() + thread.getThreadGroup());
                        MsgTask msgTask = new MsgTask();
                        msgTask.setQueueChannel(queueChannel);
                        msgTask.setDeliveryTag(envelope.getDeliveryTag());
                        msgTask.setMsg(new String(body, StandardCharsets.UTF_8));
                        work.notify(msgTask);
                        log.info("下载处理完毕 开始进行 mq ack 操作");
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    } finally {
                        long deliveryTag = -1;
                        try {
                            deliveryTag = envelope.getDeliveryTag();
                            //是否是手动模式
                        } finally {
                            try {
                                if (ack != null) {
                                    ack.doAck(channel, deliveryTag, consumerTag);
                                } else if (autoAck) {
                                    MqUtil.ack(channel, deliveryTag);
                                }
                            } catch (Exception e) {
                                log.error(e.getMessage(), e);
                            }//此处存在，主动关闭channel时，可能已经有消息进来了
                            if (closed) {
                                MqUtil.closeChannel(channel);
                            }
                        }
                    }
                }

                @Override
                public void handleConsumeOk(String consumerTag) {
                    log.info("handleConsumeOk-->{}", consumerTag);
                }

                @Override
                public void handleCancelOk(String s) {
                    log.info("handleCancelOk-->{}", s);
                }

                @Override
                public void handleCancel(String s) throws IOException {
                    log.info("handleCancel-->{}", s);
                    //如果有异常发生需要从连接池中去除
                    //MqUtil.closeChannel(channel);
                    // RabbitMqPool.me().back(node);
                }

                @Override
                public void handleShutdownSignal(String s, ShutdownSignalException e) {
                    log.error("handleShutdownSignal-->{}{}", s, e.getMessage(), e);
                    log.error("consumer close error!");
                    //如果有异常发生需要从连接池中去除
                    // MqUtil.closeChannel(channel);
                    // RabbitMqPool.me().back(node);
                }

                @Override
                public void handleRecoverOk(String s) {
                    log.info("handleRecoverOk-->{}", s);
                }
            });

        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return true;
    }
}
