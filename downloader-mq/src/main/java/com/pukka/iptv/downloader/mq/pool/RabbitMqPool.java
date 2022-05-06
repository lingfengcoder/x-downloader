package com.pukka.iptv.downloader.mq.pool;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.pukka.iptv.downloader.mq.model.QueueInfo;
import com.pukka.iptv.downloader.pool.AbstractPool;
import com.pukka.iptv.downloader.pool.Key;
import com.pukka.iptv.downloader.pool.Node;
import com.pukka.iptv.downloader.pool.PoolConfig;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import static com.pukka.iptv.downloader.mq.pool.MqConnection.*;

/**
 * @Author: wz
 * @Date: 2021/12/17 14:44
 * @Description: 基于动态连接池模型的RabbitMq的连接池 支持动态调整连接数，多线程复用连接等
 */
@Slf4j
public class RabbitMqPool extends AbstractPool<RabbitMqPool.RKey, Channel, Node<RabbitMqPool.RKey, Channel>> {
    //连接池
    private final static Map<RKey, Collection<Node<RKey, Channel>>> pool = new HashMap<>();
    //并发锁
    private final static ReentrantLock lock = new ReentrantLock(true);
    //每个FTP　核心限制数
    private final static int PER_CORE_LIMIT = 5;
    //池子最大限制数
    private final static int MAX_LIMIT = NO_LIMIT;
    //每个keyPool最大连接数
    private final static int PER_MAX_LIMIT = 10;
    //最大空闲存活时间　5s
    private final static long MAX_LIVE_TIME = 5000;
    //最大等待队列长度
    private final static int MAX_AWAIT_QUEUE_LENGTH = 1024;


    private RabbitMqPool() {
        super();
    }

    private static final RabbitMqPool me = new RabbitMqPool();

    public static RabbitMqPool me() {
        return me;
    }


    @Override
    protected PoolConfig<RabbitMqPool.RKey, Channel, Node<RabbitMqPool.RKey, Channel>> getPoolConfig() {
        return new PoolConfig<RabbitMqPool.RKey, Channel, Node<RabbitMqPool.RKey, Channel>>()
                .setPool(pool).setLock(lock).setEnableSchedule(false)
                .setMaxFreeNodeLiveTime(MAX_LIVE_TIME)
                .setMaxLiveNodeLimit(MAX_LIMIT)
                .setAwaitQueueLength(MAX_AWAIT_QUEUE_LENGTH);
    }

    private volatile Connection connection = null;

    @Override
    //创建一个连接
    protected Node<RKey, Channel> generalConnect(RKey key) {
        Channel channel = null;
        try {
            if (connection == null) {
                MqConnection mqBean = SpringUtil.getBean(MqConnection.class);
                connection = mqBean.getConnection();
            }
            channel = connection.createChannel();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (channel != null) {
            return Node.node(key, channel);
        }
        return null;
    }

    @Override
    //关闭一个连接
    protected boolean closeConnect(Node<RKey, Channel> node) {
        Channel channel = node.getClient();
        return MqUtil.closeChannel(channel);
    }


    @Override
    //测试连接是否可用
    public boolean nodeIsOpen(Node<RKey, Channel> node) {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }
        Channel client = node.getClient();
        return client.isOpen();
    }

    //刷新池子中最大的连接数
    public void refreshPoolMaxSize(int limit) {
        PoolConfig<RKey, Channel, Node<RKey, Channel>> config =
                new PoolConfig<RKey, Channel, Node<RKey, Channel>>().setMaxLiveNodeLimit(limit);
        //调整连接池配置文件
        super.refreshPoolConfig(config);
    }

    //刷新单key的连接数
    public void balanceKeyPoolSize(RKey key) {
        //key原本配置的节点最大连接数
        int realSize = super.getPoolRealSize(key);
        //如果新调整的连接数和原本配置的不同，需要进行调整
        if (key.getLimit() != realSize) {
            //设置单key的连接数配置
            super.forceRefreshKeyPoolLimit(key, key.getLimit());
            //主动调节
            super.balanceKeyPool(key);
        }
    }

    //如果连接池的limit有调整，检查是否有必要去关闭指定连接
    public void closeNodeIfNecessary(RKey key, Node<RKey, Channel> node) {
        super.closeNodeIfNecessary(key, node);
    }

    @Setter
    @Getter
    @Accessors(chain = true)
    //连接的唯一标识
    public static class RKey implements Key<RKey> {
        @Override
        public int getLimit() {
            return limit;
        }

        public RKey setLimit(int limit) {
            this.limit = limit;
            return this;
        }

        @Override
        public RKey cloneMe() {
            RKey rKey = new RKey();
            rKey.setLimit(this.limit);
            rKey.setName(this.name);
            // return this.clone();
            return rKey;
        }


        private String name;
        private int limit;

        private RKey() {
        }

        public static RKey newKey(QueueInfo info) {
            RKey rKey = new RKey().setName(parseKeyName(info));
            rKey.setLimit(NO_LIMIT);
            return rKey;
        }

        public static RKey newKey(QueueInfo info, int limit) {
            RKey rKey = new RKey().setName(parseKeyName(info));
            rKey.setLimit(limit);
            return rKey;
        }

        //消费者只使用queue　　
        //生产者需要使用exchange和routeKey
        public static String parseKeyName(QueueInfo info) {
            if (!ObjectUtil.isEmpty(info.queue())) {
                return QUEUE + ":" + info.queue();
            } else if (!ObjectUtil.isEmpty(info.exchange()) && !ObjectUtil.isEmpty(info.routeKey())) {
                return EXCHANGE + ":" + info.exchange() + ROUTE_KEY + ":" + info.routeKey();
            } else {
                throw new RuntimeException("信息丢失，queue/exchange/routeKey 不可都为空！");
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RKey key = (RKey) o;
            return Objects.equals(name, key.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }
}
