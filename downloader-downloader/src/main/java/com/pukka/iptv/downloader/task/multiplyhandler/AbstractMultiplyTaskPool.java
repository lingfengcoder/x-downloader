package com.pukka.iptv.downloader.task.multiplyhandler;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;

/**
 * @Author: wz
 * @Date: 2022/1/4 16:15
 * @Description: 双队列模式的任务 多线程模型
 */
@Slf4j
public abstract class AbstractMultiplyTaskPool<T> implements MultiplyTaskPool<T> {

    //不限制的标识
    public final static int NO_LIMIT = -1;

    protected abstract boolean doWork(T task);

    protected abstract int waitQueueLimit();

    //队列和锁不设置成内置的原因是，为了外部使用可以决定是否共享队列和锁
    //如果内置，可能会存在每个实现类都有各自的队列和锁
    protected abstract Lock getLock();

    //等待队列
    protected abstract PriorityQueue<T> getWaitQueue();

    //工作队列
    protected abstract PriorityQueue<T> getWorkingQueue();

    //-1 不限制
    protected abstract int workQueueLimit();

    protected abstract ExecutorService getExecutor();

    @Override
    //获取多处理器中所有的任务个数
    public int queryTaskCount() {
        Lock lock = getLock();
        try {
            lock.lock();
            PriorityQueue<T> waitQueue = getWaitQueue();
            PriorityQueue<T> workingQueue = getWorkingQueue();
            return waitQueue.size() + workingQueue.size();
        } finally {
            lock.unlock();
        }
    }

    @Override
    //获取工作的任务个数
    public int queryWorkingTaskCount() {
        Lock lock = getLock();
        try {
            lock.lock();
            PriorityQueue<T> workingQueue = getWorkingQueue();
            return workingQueue.size();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean addTask(T task) {
        Lock lock = getLock();
        try {
            lock.lock();
            PriorityQueue<T> waitQueue = getWaitQueue();
            int limit = waitQueueLimit();
            if (limit != NO_LIMIT && queryTaskCount() >= limit) {
                return false;
            }  //添加到待下载队列中去
            waitQueue.add(task);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        } finally {
            lock.unlock();
        }
        return true;
    }


    @Override
    public List<T> getTask(int count) {
        T task;
        Lock lock = getLock();
        try {
            if (count <= 0) {
                return null;
            }
            lock.lock();
            List<T> result = new ArrayList<>(count);
            PriorityQueue<T> waitQueue = getWaitQueue();
            for (int i = 0; i < count; i++) {
                //读写要保证原子性
                //从待下载队列获取数据，加入到下载中队列
                task = waitQueue.poll();
                if (task == null) return result;
                //添加到下载中队列
                boolean fine = addTaskToWorkingQueue(task);
                if (!fine) {
                    //添加失败 工作队列已经满了
                    break;
                }
                result.add(task);
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    //工作队列添加任务
    private boolean addTaskToWorkingQueue(T task) {
        Lock lock = getLock();
        try {
            lock.lock();
            int limit = workQueueLimit();
            PriorityQueue<T> workingQueue = getWorkingQueue();
            if (limit != NO_LIMIT && workingQueue.size() >= limit) {
                return false;
            }
            workingQueue.add(task);
        } finally {
            lock.unlock();
        }
        return true;
    }

    @Override
    public boolean submitTask() {
        try {
            ExecutorService executor = getExecutor();
            executor.execute(() -> {
                while (true) {
                    //不停的去拉取任务
                    List<T> list = getTask(1);
                    if (list.size() == 0) {
                        //没有任务就跳出,等待新的任务分配
                        break;
                    }
                    T task = list.get(0);
                    try {
                        //做任务
                        doWork(task);
                    } catch (Exception e) {
                        log.error(e.getMessage());
                    } finally {
                        finishTask(task);
                    }
                    //缓口气
                    Thread.yield();
                }
            });
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
        return true;
    }

    @Override
    public void cancelTask(T task) {
        Lock lock = getLock();
        try {
            lock.lock();
            PriorityQueue<T> waitQueue = getWaitQueue();
            waitQueue.remove(task);
        } finally {
            lock.unlock();
        }
    }

    private void finishTask(T task) {
        if (task == null) return;
        Lock lock = getLock();
        try {
            lock.lock();
            //执行完毕的任务从队列中剔除
            PriorityQueue<T> workingQueue = getWorkingQueue();
            workingQueue.remove(task);
        } finally {
            lock.unlock();
        }
    }

}
