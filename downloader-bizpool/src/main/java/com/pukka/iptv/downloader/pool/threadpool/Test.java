package com.pukka.iptv.downloader.pool.threadpool;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

@Slf4j
public class Test {
    public static void main(String[] args) {

        Object lock = new Object();
        Thread thread1 = new Thread(() -> {
            synchronized (lock) {
                //while (true) {
                try {
                    Thread me = Thread.currentThread();
                    log.info("me1 start");
                    me.interrupt();
                    lock.wait();
                    log.info("me1 end");
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
                // }
            }
        }, "thread-1");
        try {
            TimeUnit.MILLISECONDS.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Thread thread2 = new Thread(() -> {
            synchronized (lock) {
                try {
                    Thread me = Thread.currentThread();
                    log.info("me2 start");
                    me.sleep(6000);
                    log.info(" object.notify() 唤醒1 ");
                    lock.notify();
                    log.info("me2 end");
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }, "thread-2");
        thread1.start();

        try {
            TimeUnit.MILLISECONDS.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        thread2.start();

        try {
            TimeUnit.MILLISECONDS.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.info("1 中断");
        //thread1.interrupt();

    }

    // try {
    //                    Thread me = Thread.currentThread();
    //                    log.info("me1 start");
    //                    HashMap<Integer, Integer> data = new HashMap<>();
    //                    int y = Integer.MAX_VALUE;
    //                    float f = 0.0F;
    //                    for (int n = 0; n < y; n++) {
    //                        data.put(n, n);
    //                        if (Thread.interrupted()) {
    //                            log.info("1 get interrupt end 1 ");
    //                            return;
    //                        }
    //                    }
    //                    log.info("me1 send");
    //                } catch (Exception e) {
    //                    log.error(e.getMessage(), e);
    //                }

    public static void main1(String[] args) throws InterruptedException {
        BizThreadPool pool = build();
        new Thread(() -> {
            int x = 10000;
            for (int i = 0; i < x; i++) {
                try {
                    pool.execute(() -> {
                        Thread thread = Thread.currentThread();
                        log.info("{}开始执行", thread.getName());
                        long begin = System.currentTimeMillis();
                        int num = 4;
                        boolean flag = true;
                        while (num > 0) {
                            try {
                                --num;
                                TimeUnit.SECONDS.sleep(1);
                                boolean interrupted = thread.isInterrupted();
                                if (interrupted) {
                                    log.info("{}有中断信号", thread.getName());
                                }
                            } catch (Exception e) {
                                log.error(e.getMessage(), e);
                                throw new RuntimeException();
                            }
                        }
                        log.info("{}执行完毕 耗时:{}", thread.getName(), (System.currentTimeMillis() - begin));
                    });
                } catch (Exception e) {
                    log.error(e.getMessage());
                    try {
                        TimeUnit.SECONDS.sleep(500);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    i--;
                }
            }
        }).start();

        TimeUnit.SECONDS.sleep(4);
        log.info("增大了核心线程数");
        pool.setCoreSize(1000);
        pool.setMaxSize(1000);
        log.info("增大了队列");
        pool.reSizeQueue(2000);
        pool.allowCoreThreadTimeOut(true);
        // pool.preStartAllCoreThreads();
        // Thread.currentThread().join();
    }

    private static BizThreadPool build() {
        ThreadFactory factory = ThreadFactoryBuilder.create()
                .setNamePrefix("##biz_dynamic_adjust_pool##").setDaemon(false)
                .build();
        BizThreadPool bizThreadPool = BizThreadPool.buildThreadPool(2, 2,
                10, 5, factory, new ThreadPoolExecutor.CallerRunsPolicy());
        return bizThreadPool;
    }
}
