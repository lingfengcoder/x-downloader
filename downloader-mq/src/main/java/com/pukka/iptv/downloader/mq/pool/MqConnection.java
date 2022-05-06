package com.pukka.iptv.downloader.mq.pool;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import com.pukka.iptv.downloader.mq.config.RabbitMqConfig;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author: wz
 * @Date: 2021/10/19 15:19
 * @Description: mq连接类
 */
@Slf4j
@Component
@Order(0)
public class MqConnection {
    @Autowired
    private RabbitMqConfig rabbitMqConfig;
    public static final String QUEUE = "queue";
    public static final String EXCHANGE = "exchange";
    public static final String ROUTE_KEY = "routeKey";

    private static volatile ConnectionFactory factory;
    private static volatile Connection localConnection;
    private static volatile ExecutorService executorService;

    private static final String THREAD_POOL_PREFIX = "##downloader-auto-close-pool##";
    private static final ReentrantLock lock = new ReentrantLock();

    //启动时初始化连接
    @PostConstruct
    private void init() {
        initConnectFactory();
    }

    @PreDestroy
    private void destroy() {
        //关闭连接池
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    //连接mq
    private void initConnectFactory() {
        lock.lock();
        try {
            if (factory != null) return;
            if (rabbitMqConfig.getAddresses() == null) {
                log.warn(" rabbitmq config 配置获取失败");
                return;
            }
            String[] addr = rabbitMqConfig.getAddresses().split(":");
            log.info("初始化mq连接池");
            factory = new ConnectionFactory();
            factory.setHost(addr[0]);
            factory.setPort(Integer.parseInt(addr[1]));
            factory.setUsername(rabbitMqConfig.getUsername());
            factory.setPassword(rabbitMqConfig.getPassword());
            factory.setVirtualHost(rabbitMqConfig.getVhost());
//            factory.setRequestedChannelMax(0);
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
            log.info("初始化mq连接池成功！{}", rabbitMqConfig);
        } catch (Exception e) {
            log.info("初始化mq连接池失败！{}", rabbitMqConfig);
            log.error(e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    public Connection getConnection() {
        if (localConnection == null) {
            try {
                lock.lock();
                if (localConnection == null) {
                    initConnectFactory();
                    if (factory != null) {
                        localConnection = factory.newConnection();
                    }
                }
            } catch (Exception e) {
                log.info("获取MQ连接失败！{}", rabbitMqConfig);
                log.error(e.getMessage(), e);
            } finally {
                lock.unlock();
            }
        }
        //重连
        if (localConnection != null && !localConnection.isOpen()) {
            try {
                lock.lock();
                if (!localConnection.isOpen()) {
                    localConnection = factory.newConnection();
                }
            } catch (IOException | TimeoutException e) {
                log.error(e.getMessage(), e);
            } finally {
                lock.unlock();
            }
        }
        return localConnection;
    }

}
