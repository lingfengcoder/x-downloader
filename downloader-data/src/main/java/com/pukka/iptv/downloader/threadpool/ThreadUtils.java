package com.pukka.iptv.downloader.threadpool;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;

/**
 * @Author: wz
 * @Date: 2021/10/24 22:30
 * @Description:
 */
@Slf4j
public class ThreadUtils extends ThreadUtil {


    public static boolean isNullOrDone(ScheduledFuture<?> scheduledFuture) {
        return scheduledFuture == null || scheduledFuture.isCancelled() || scheduledFuture.isDone();
    }

    //关闭线程任务
    public static void cancelSchedule(ScheduledFuture<?> scheduledFuture) {
        try {
            if (scheduledFuture != null && (!scheduledFuture.isCancelled() || !scheduledFuture.isDone())) {
                scheduledFuture.cancel(false);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public static void waitForFuture(List<Future<?>> list) {
        if (!ObjectUtil.isEmpty(list)) {
            for (Future<?> future : list) {
                try {
                    future.get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
