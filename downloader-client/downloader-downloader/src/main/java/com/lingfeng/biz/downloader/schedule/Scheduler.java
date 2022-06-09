package com.lingfeng.biz.downloader.schedule;


import com.lingfeng.biz.downloader.config.NodeConfig;
import com.lingfeng.biz.downloader.model.Downloading;

import com.lingfeng.biz.downloader.util.ThreadUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @Author: wz
 * @Date: 2022/1/4 20:54
 * @Description: 根据配置动态创建或销毁mq监听
 */
@Slf4j
@Component
public class Scheduler {
    @Resource(name = "downloaderScheduleThreadPool")
    private ScheduledThreadPoolExecutor downloaderScheduleThreadPool;

    @Autowired
    private NodeConfig nodeConfig;

    private volatile ScheduledFuture<?> scheduledFuture = null;

    private volatile ScheduledFuture<?> tmpIndexScheduleFuture = null;

    private final static long BALANCE_TIMEOUT = 60000L;//配置调整超时时间30s


    //下载节点初始化
    @PostConstruct
    private void init() {
        tmpIndexSchedule();
    }


    // @Override//NACOS 配置改变进行处理
    public void configRefreshEvent() {

        tmpIndexSchedule();
    }

    //临时下载文件的 定时清理器
    private void tmpIndexSchedule() {
        // log.info("getTmpIndexTimeCron={}", nodeConfig.getTmpIndexTimeCron());
        log.info("【tmpIndexSchedule】设置清理临时下载文件的周期时间={}", nodeConfig.getTmpIndexLiveTime());
        Long liveTime = nodeConfig.getTmpIndexLiveTime();
        if (tmpIndexScheduleFuture != null) {
            ThreadUtils.cancelSchedule(tmpIndexScheduleFuture);
        }
        tmpIndexScheduleFuture = downloaderScheduleThreadPool
                .scheduleAtFixedRate(() -> Downloading.clearTmpIndex(liveTime),
                        1, liveTime, TimeUnit.MILLISECONDS);
    }

}
