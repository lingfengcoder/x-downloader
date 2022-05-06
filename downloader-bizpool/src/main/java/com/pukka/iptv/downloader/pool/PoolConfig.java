package com.pukka.iptv.downloader.pool;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author: wz
 * @Date: 2021/12/16 14:51
 * @Description: 连接池的配置类 具有构建／克隆／检测配置等功能
 * 因为考虑到安全性，所以在读取配置时进行了对象信息的复制，防止外部直接修改属性导致内部异常
 */
@Getter
@Setter
@Accessors(chain = true)
public class PoolConfig<K extends Key<K>, C, N extends Node<K, C>> {
    //默认数据
    //默认连接允许最大空闲时间
    public final static long DEFAULT_MAX_FREE_NODE_LIVE_TIME = 5000L;
    //默认所有最大连接数
    public final static int DEFAULT_MAX_NODE_LIMIT = 1024;
    //默认单个KEY　在池中允许的常驻存活连接数
    public final static int DEFAULT_CORE_LIVE_NODE_LIMIT = 5;
    //默认单个KEY　在池中允许等待该KEY连接的　最大等待线程数　
    public final static int DEFAULT_AWAIT_QUEUE_LENGTH = 50;
    //是否在池中维护一定量的存活连接
    public final static boolean DEFAULT_OPEN_CORE_NODE = false;
    //默认不开启定时清理过期没使用的连接
    public final static boolean DEFAULT_ENABLE_SCHEDULE = false;
    //默认开启连接池
    public final static boolean DEFAULT_ENABLE = true;
    //名称
    private String name;
    //连接池
    private volatile Map<K, Collection<N>> pool;
    //安全锁
    private volatile ReentrantLock lock;
    //每个连接最大空闲时间
    private Long maxFreeNodeLiveTime;
    //连直接最大连接数
    private Integer maxLiveNodeLimit;
    //单个key等待队列最大等待个数
    private Integer awaitQueueLength;
    //是否需要核心连接 //默认不需要核心连接 在连接空闲不使用的时候会定期关闭连接
    private Boolean openCoreNode;
    //是否开启 false 关闭连接池
    private Boolean enable;
    //是否开启定时任务 默认不开启定时清理空闲连接
    private Boolean enableSchedule;

    //缺值情况下使用默认值填充
    public static <K extends Key<K>, C, N extends Node<K, C>> PoolConfig<K, C, N> buildNewConfig(PoolConfig<K, C, N> config) {
        PoolConfig<K, C, N> newConfig = new PoolConfig<>();
        newConfig.name = config.name;
        newConfig.maxFreeNodeLiveTime = defaultVal(config.maxFreeNodeLiveTime, DEFAULT_MAX_FREE_NODE_LIVE_TIME);
        newConfig.maxLiveNodeLimit = defaultVal(config.maxLiveNodeLimit, DEFAULT_MAX_NODE_LIMIT);
        newConfig.awaitQueueLength = defaultVal(config.awaitQueueLength, DEFAULT_AWAIT_QUEUE_LENGTH);
        //开关
        newConfig.openCoreNode = defaultVal(config.openCoreNode, DEFAULT_OPEN_CORE_NODE);
        newConfig.enableSchedule = defaultVal(config.enableSchedule, DEFAULT_ENABLE_SCHEDULE);
        newConfig.enable = defaultVal(config.enable, DEFAULT_ENABLE);
        newConfig.lock = config.lock;
        newConfig.pool = config.pool;
        check(newConfig);
        return newConfig;
    }

    //刷新配置
    public static <K extends Key<K>, C, N extends Node<K, C>> PoolConfig<K, C, N> refresh(PoolConfig<K, C, N> newConfig, PoolConfig<K, C, N> oldConfig) {
        oldConfig.maxFreeNodeLiveTime = defaultVal(newConfig.maxFreeNodeLiveTime, oldConfig.maxFreeNodeLiveTime);
        oldConfig.maxLiveNodeLimit = defaultVal(newConfig.maxLiveNodeLimit, oldConfig.maxLiveNodeLimit);
        oldConfig.awaitQueueLength = defaultVal(newConfig.awaitQueueLength, oldConfig.awaitQueueLength);
        oldConfig.openCoreNode = defaultVal(newConfig.openCoreNode, oldConfig.openCoreNode);
        oldConfig.enableSchedule = defaultVal(newConfig.enableSchedule, oldConfig.enableSchedule);
        check(oldConfig);
        return oldConfig;
    }

    //检查配置是否有不合法的数据
    private static <K extends Key<K>, C, N extends Node<K, C>> void check(PoolConfig<K, C, N> config) {
        if (config.awaitQueueLength <= 0) {
            config.awaitQueueLength = DEFAULT_AWAIT_QUEUE_LENGTH;
        }
    }

    private static <T> T defaultVal(T val, T def) {
        return val != null ? val : def;
    }

    //复制基础属性
    public static <K extends Key<K>, C, N extends Node<K, C>> PoolConfig<K, C, N> copyConfig(PoolConfig<K, C, N> config) {
        PoolConfig<K, C, N> newConfig = new PoolConfig<>();
        newConfig.maxFreeNodeLiveTime = config.maxFreeNodeLiveTime;
        newConfig.maxLiveNodeLimit = (config.maxLiveNodeLimit);
        newConfig.awaitQueueLength = (config.awaitQueueLength);
        newConfig.openCoreNode = (config.openCoreNode);
        newConfig.enable = config.enable;
        return newConfig;
    }

    //复制基础属性
    public static <K extends Key<K>, C, N extends Node<K, C>> PoolConfig<K, C, N> copyConfig(PoolConfig<K, C, N> newConfig, PoolConfig<K, C, N> oldConfig) {
        newConfig.maxFreeNodeLiveTime = oldConfig.maxFreeNodeLiveTime;
        newConfig.maxLiveNodeLimit = (oldConfig.maxLiveNodeLimit);
        newConfig.awaitQueueLength = (oldConfig.awaitQueueLength);
        newConfig.openCoreNode = (oldConfig.openCoreNode);
        newConfig.enable = oldConfig.enable;
        return newConfig;
    }
}
