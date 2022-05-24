package com.lingfeng.biz.server.task;

import com.lingfeng.biz.downloader.log.BizLog;
import com.lingfeng.biz.downloader.model.DownloadTask;
import com.lingfeng.biz.downloader.model.NodeRemain;
import com.lingfeng.biz.server.DownloaderServer;
import com.lingfeng.biz.server.cache.WaterCacheQueue;
import com.lingfeng.biz.server.config.DispatcherConfig;
import com.lingfeng.biz.server.policy.DeliverPolicy;
import com.lingfeng.dutation.store.StoreApi;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

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
    //上次发送时间
    private AtomicLong lastSendTime = new AtomicLong();
    //最大等待时间
    private int maxWaitTime = 5 * 1000;//5s
    //缓冲队列最大长度
    private int maxCacheSize = 50;
    //缓冲任务队列
    private WaterCacheQueue<DownloadTask> cacheQueue;
    //执行锁
    private ReentrantLock lock;
    //策略类
    private DeliverPolicy<NodeRemain, DownloadTask> deliverPolicy;
    //执行器线程池
    private ThreadPoolTaskExecutor executorPool;
    //上一次优先级高的队列 0:下载队列 1:重试队列
    private int lastDice;
    private DispatcherConfig dispatcherConfig;

    private DownloaderServer downloaderServer;

    private BizLog log;

    private StoreApi<DownloadTask> dbStore;

    public void throwDice() {
        this.lastDice = this.lastDice == 0 ? 1 : 0;
    }
}
