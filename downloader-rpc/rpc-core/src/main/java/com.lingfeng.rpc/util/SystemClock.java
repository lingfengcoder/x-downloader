package com.lingfeng.rpc.util;


import java.util.Date;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.lingfeng.rpc.util.TimeUtil.StandardFormat;


/**
 * @author: wz
 * @Date: 2019/10/30 16:15
 * @Description:
 */
public class SystemClock {
    private final long period;

    private final AtomicLong now = new AtomicLong(0);

    private SystemClock(long period) {
        this.period = period;
        scheduleClockUpdating();
    }

    private static SystemClock instance() {
        return InstanceHolder.INSTANCE;
    }

    public static long now() {
        return instance().currentTimeMillis();
    }

    public static String nowDate() {
        Long timeL = instance().currentTimeMillis();
        return TimeUtil.formatDate(timeL, StandardFormat);
    }

    public static Date nowTime() {
        return new Date(now());
    }

    /**
     * @Description: 提交一个定时任务循环获取System.currentTimeMillis()
     * @Param: []
     * @Return: void
     * @author: wz
     * @Date: 2019/10/30 16:18
     */
    private void scheduleClockUpdating() {
//        try {
//            ScheduledThreadPoolExecutor scheduledExecutor = SpringUtils.getBean("scheduledExecutorService");
//            Thread thread = new Thread(() -> now.set(System.currentTimeMillis()), "System Clock");
//            thread.setDaemon(true);
//            scheduledExecutor.scheduleAtFixedRate(thread, period, period, TimeUnit.MILLISECONDS);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1, r -> {
            Thread thread = new Thread(r, "System Clock");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleAtFixedRate(() -> now.set(System.currentTimeMillis()), period, period, TimeUnit.MILLISECONDS);
    }

    /**
     * @Description: 可能第一次调用可能时间不精准
     * @Param: []
     * @Return: long
     * @author: wz
     * @Date: 2019/10/30 16:20
     */
    private long currentTimeMillis() {
        long time = now.get();
        return time == 0 ? System.currentTimeMillis() : time;
    }

    private static class InstanceHolder {
        public static final SystemClock INSTANCE = new SystemClock(1);
    }
}
