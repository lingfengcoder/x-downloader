package com.pukka.iptv.downloader.task;

import com.pukka.iptv.downloader.config.DispatcherConfig;
import com.pukka.iptv.downloader.log.BizLog;
import com.pukka.iptv.downloader.mq.config.QueueConfig;
import com.pukka.iptv.downloader.mq.model.MsgTask;
import com.pukka.iptv.downloader.mq.model.QueueInfo;
import com.pukka.iptv.downloader.nacos.NacosService;
import com.pukka.iptv.downloader.policy.DeliverPolicy;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author: wz
 * @Date: 2021/10/22 09:57
 * @Description: 处理器元信息
 */
@Getter
@Setter
@Accessors(fluent = true)
public class MetaHead {
    //名称
    private String name;
    //监听的队列信息
    private QueueInfo queue;
    //上次发送时间
    private AtomicLong lastSendTime = new AtomicLong();
    //最大等待时间
    private int maxWaitTime = 5 * 1000;//5s
    //缓冲队列最大长度
    private int maxCacheSize = 50;
    //缓冲任务队列
    private List<MsgTask> cacheList;
    //执行锁
    private ReentrantLock lock;
    //策略类
    private DeliverPolicy<QueueInfo, MsgTask> deliverPolicy;
    //执行器线程池
    private ExecutorService executorPool;
    //上一次优先级高的队列 0:下载队列 1:重试队列
    private int lastDice;
    //一些配置
    private QueueConfig queueConfig;
    private DispatcherConfig dispatcherConfig;
    private NacosService nacosService;
    private BizLog log;

    public void throwDice() {
        this.lastDice = this.lastDice == 0 ? 1 : 0;
    }
}
