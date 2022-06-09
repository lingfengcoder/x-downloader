package com.lingfeng.biz.server.schedule;


import com.lingfeng.biz.downloader.util.ThreadUtils;
import com.lingfeng.biz.server.config.DispatcherConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * @Author: wz
 * @Date: 2021/10/15 11:41
 * @Description: 调度中心  负责提供暴漏调度接口和内部定时，负责收集和触发组件内所有的调度处理器
 */
@Slf4j
@Component
public class ScheduleCenter {
    @Autowired
    @Qualifier("dispatcherScheduleThreadPool")
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
    @Autowired
    private DispatcherConfig config;
    //定时任务线程池future
    private volatile ScheduledFuture<?> scheduledFuture = null;

    @PostConstruct
    private void init() {
        //启动时就判断是否启动内部定时任务
        if (config.getEnableSchedule()) {
            // if (true) {
            doSchedule();
        }
    }

    //@Override//接收nacos配置刷新的事件
    public void configRefreshEvent() {
        if (config.getEnableSchedule()) {
            log.info("开启 调度器的内置定时任务");
            doSchedule();
        } else {
            log.info("关闭 调度器的内置定时任务");
            ThreadUtils.cancelSchedule(scheduledFuture);
        }
    }

    //执行内部定时任务
    private void doSchedule() {
        // 内部定时任务执行 每秒执行一次
        if (ThreadUtils.isNullOrDone(scheduledFuture)) {
            scheduledFuture =
                    scheduledThreadPoolExecutor.scheduleAtFixedRate(new DispatcherJob(config.getEnable()), 1, config.getIntervalTime(), TimeUnit.MILLISECONDS);
        }
    }


}
