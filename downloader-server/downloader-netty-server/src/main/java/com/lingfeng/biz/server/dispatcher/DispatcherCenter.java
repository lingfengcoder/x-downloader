package com.lingfeng.biz.server.dispatcher;


import cn.hutool.extra.spring.SpringUtil;
import com.lingfeng.biz.downloader.log.BizLog;
import com.lingfeng.biz.downloader.threadpool.ThreadUtils;
import com.lingfeng.biz.server.DownloaderServer;
import com.lingfeng.biz.server.config.DispatcherConfig;
import com.lingfeng.biz.server.task.TaskHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Map;
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
public class DispatcherCenter {
    @Autowired
    private DownloaderServer downloaderServer;
    @Resource(name = "dispatcherScheduleThreadPool")
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
                    scheduledThreadPoolExecutor.scheduleAtFixedRate(this::dispatch, 1, config.getIntervalTime(), TimeUnit.MILLISECONDS);
        }
    }

    //执行调度
    public void dispatch() {
        if (config.getEnable()) {
            doWork();
        }
    }


    private void doWork() {
        ApplicationContext app = SpringUtil.getApplicationContext();
        if (app != null) {
            log.info("下载模块 调度中心开始执行调度");
            //找出容器中所有的处理器
            Map<String, TaskHandler> taskHandler = app.getBeansOfType(TaskHandler.class);
            for (Map.Entry<String, TaskHandler> item : taskHandler.entrySet()) {
                try {
                    item.getValue().handler();
                    //executor.execute(() -> item.getValue().handler());
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }
        }
    }

}
