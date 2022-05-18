package com.lingfeng.biz.downloader.task.multiplyhandler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;

/**
 * @Author: wz
 * @Date: 2022/1/4 16:15
 * @Description: 双队列模式的任务 多线程模型
 */
@Slf4j
public abstract class AbstractMultiplyTaskPool<T> implements MultiplyTaskPool<T> {
    private final static AtomicInteger limit = new AtomicInteger(1);

    protected abstract boolean doWork(T task);

    //队列和锁不设置成内置的原因是，为了外部使用可以决定是否共享队列和锁
    //如果内置，可能会存在每个实现类都有各自的队列和锁
    protected abstract Lock getLock();

    protected abstract ThreadPoolTaskExecutor getExecutor();

    @Override
    public boolean submitTask(T task) {
        Lock lock = getLock();
        ThreadPoolTaskExecutor executor = getExecutor();
        try {
            lock.lock();
            int poolSize = executor.getPoolSize();
            int activeCount = executor.getActiveCount();
            if (poolSize > getLimit()) {
                log.info(" poolSize > Limit{} ", poolSize);
                //return false;
            }
            log.info("executor.getActiveCount() ={}", activeCount);
            if (poolSize < getLimit() || activeCount < getLimit()) {
                try {
                    executor.execute(() -> doWork(task));
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    return false;
                }
                return true;
            }
        } finally {
            lock.unlock();
        }
        return false;
    }

    @Override
    public int getAliveTaskCount() {
        ThreadPoolTaskExecutor executor = getExecutor();
        return executor.getPoolSize();
    }

    @Override
    public void cancelTask(T task) {

    }

    @Override
    public int getLimit() {
        return limit.get();
    }

    @Override
    public void setLimit(int x) {
        limit.set(x);
    }
}
