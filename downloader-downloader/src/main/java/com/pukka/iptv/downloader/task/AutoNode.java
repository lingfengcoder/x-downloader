package com.pukka.iptv.downloader.task;

import com.pukka.iptv.downloader.config.NodeConfig;
import com.pukka.iptv.downloader.model.DNode;
import com.pukka.iptv.downloader.mq.config.QueueConfig;
import com.pukka.iptv.downloader.mq.model.QueueInfo;
import com.pukka.iptv.downloader.mq.pool.MqUtil;
import com.pukka.iptv.downloader.mq.pool.RabbitMqPool;
import com.pukka.iptv.downloader.nacos.NacosService;
import com.pukka.iptv.downloader.pool.Node;
import com.rabbitmq.client.Channel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;


/**
 * @Author: wz
 * @Date: 2021/10/13 18:03
 * @Description: 自动创建下载接节点
 */
@Getter
@Slf4j
@Component
public class AutoNode {
    @Autowired
    private NacosService nacosServer;
    @Autowired
    private QueueConfig queueConfig;
    @Autowired
    private NodeConfig nodeConfig;
    //队列是否声明成功
    private static final AtomicBoolean declare = new AtomicBoolean(false);

    public DNode defaultDownloadNode() {
        return createNode();
    }

    private boolean declareQueue(QueueInfo queue) {
        if (queue == null) return false;
        Integer limit = nodeConfig.getChannelLimit();
        if (!nodeConfig.isEnable()) {
            limit = 0;
        }
        RabbitMqPool.RKey key = RabbitMqPool.RKey.newKey(queue, limit);
        Node<RabbitMqPool.RKey, Channel> node = RabbitMqPool.me().pickNonBlock(key);
        if (node != null) {
            try {
                Channel channel = node.getClient();
                declare.set(MqUtil.declareOrBindQueueAndExchange(channel, queue));
            } finally {
                //退还并关闭连接
                RabbitMqPool.me().backClose(node);
            }
        }
        return declare.get();
    }


    //创建
    public DNode createNode() {
        QueueInfo queueInfo;
        try {
            //从nacos中获取 instanceId
            String instanceId = nacosServer.getClearInstanceId();
            queueInfo = createQueue(instanceId);
            if (!declare.get()) {
                //声明队列
                if (!declareQueue(queueInfo)) {
                    log.error("队列声明失败！{}", queueInfo);
                    return null;
                }
                log.info("队列声明成功！");
            }
            return new DNode().setQueueInfo(queueInfo).setInstanceId(instanceId);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    private QueueInfo createQueue(String instanceId) {
        if (instanceId == null) return null;
        String queue = queueConfig.getExecuteQueuePrefix() + instanceId;
        return new QueueInfo()
                .queue(queue) //队列名
                .routeKey(instanceId)//路由key
                .exchange(queueConfig.getExecuteExchange());//交换机
    }
}
