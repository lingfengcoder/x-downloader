package com.pukka.iptv.downloader.mq.pool;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import cn.hutool.core.util.RandomUtil;
import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author: wz
 * @Date: 2022/4/26 18:22
 * @Description:
 */
@Slf4j
public class Demo {

    private final static int MAX_ACK_COUNT = 5;

    public static void main(String[] args) throws Exception {
        Channel channel1 = Connection.generalChannel();
        int x = 30;
        for (int i = 0; i < x; i++) {
            Producer.make(channel1, "" + i);
        }
        //   channel1.basicQos(1);
        //  Consumer.listen(channel1, true);

        Channel channel2 = Connection.generalChannel();
        channel2.basicQos(10);
        Consumer.listen(channel2, true);

        // Channel channel3 = Connection.generalChannel();
        // channel3.basicQos(1);
        //  Consumer.listen(channel3, false);
    }


    static class Connection {
        private static volatile ConnectionFactory factory;
        private static volatile com.rabbitmq.client.Connection localConnection;
        private static volatile ExecutorService executorService;
        private static final String THREAD_POOL_PREFIX = "##downloader-auto-close-pool##";

        static {
            initConnectFactory();
        }

        public static synchronized com.rabbitmq.client.Connection getConnection() throws IOException, TimeoutException {
            if (localConnection == null) {
                localConnection = factory.newConnection();
            }
            return localConnection;
        }

        private static void initConnectFactory() {
            try {
                String address = "127.0.0.1";
                int port = 5672;
                String usename = "admin";
                String pwd = "admin";
                String vhost = "download";
                if (factory != null) return;
                log.info("初始化mq连接池");
                factory = new ConnectionFactory();
                factory.setHost(address);
                factory.setPort(port);
                factory.setUsername(usename);
                factory.setPassword(pwd);
                factory.setVirtualHost(vhost);

                factory.setRequestedFrameMax(0);
                factory.setAutomaticRecoveryEnabled(true);
                factory.setNetworkRecoveryInterval(10000); //attempt recovery every 10 seconds
                factory.setConnectionTimeout(1000 * 10);//30s
                // 设置心跳为60秒
                factory.setRequestedHeartbeat(20);//60s

                ThreadFactory threadFactory = ThreadFactoryBuilder.create()
                        .setDaemon(false).setNamePrefix(THREAD_POOL_PREFIX).build();
                //支持自动清理的线程池 超过60s没有使用就会被回收
                executorService = Executors.newCachedThreadPool(threadFactory);
                factory.setSharedExecutor(executorService);
                factory.setThreadFactory(threadFactory);

            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        private static Channel generalChannel() throws Exception {
            com.rabbitmq.client.Connection connection = Connection.getConnection();
            return connection.createChannel();
        }
    }

    private final static String exchange = "downloader_direct_exchange";
    private final static String queue = "downloader_task_queue";
    private final static String route = "downloader_task_queue";

    static class Producer {
        private static void make(Channel channel, String data) throws Exception {
            channel.basicPublish(exchange, route, null, data.getBytes(StandardCharsets.UTF_8));
            channel.addConfirmListener(new ConfirmListener() {
                @Override
                public void handleAck(long deliveryTag, boolean multiple) throws IOException {
                    log.info("channel id:{} deliveryTag:{} msg:{} multiple:{}", channel.getChannelNumber(), deliveryTag, data, multiple);
                }

                @Override
                public void handleNack(long deliveryTag, boolean multiple) throws IOException {
                    log.info("channel id:{} deliveryTag:{} msg:{} multiple:{}", channel.getChannelNumber(), deliveryTag, data, multiple);
                }
            });
        }
    }

    static class Consumer {

        private final static AtomicInteger ACK_COUNT = new AtomicInteger(0);

        private static void listen(Channel channel, boolean autoAck) throws Exception {

            channel.basicConsume(queue, false, new com.rabbitmq.client.Consumer() {
                @Override
                public void handleConsumeOk(String consumerTag) {
                    log.info("consumer:{} handleConsumeOk", consumerTag);
                }

                @Override
                public void handleCancelOk(String consumerTag) {
                    log.info("consumer:{} handleCancelOk", consumerTag);
                }

                @Override
                public void handleCancel(String consumerTag) throws IOException {
                    log.info("consumer:{} handleCancel", consumerTag);
                }

                @Override
                public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {
                    log.info("consumer:{} handleShutdownSignal", consumerTag);
                }

                @Override
                public void handleRecoverOk(String consumerTag) {
                    log.info("consumer:{} handleRecoverOk", consumerTag);
                }

                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    String msg = new String(body, StandardCharsets.UTF_8);
                    log.info("consumer:{} 收到了消息:{}", consumerTag, msg);
                    log.info("envelope:{}", envelope);
                    log.info("messageCount:{}", channel.messageCount(queue));

                    try {
                        TimeUnit.MILLISECONDS.sleep(RandomUtil.randomInt(200, 300));
                    } catch (InterruptedException e) {
                        log.error(e.getMessage(), e);
                    }
                    if (autoAck) {
                        if (ACK_COUNT.get() < MAX_ACK_COUNT) {
                            channel.basicAck(envelope.getDeliveryTag(), false);
                            ACK_COUNT.incrementAndGet();
                        }
                    }
                }
            });
        }
    }
}
