package com.lingfeng.biz.server.task;


import com.lingfeng.biz.downloader.model.*;
import com.lingfeng.biz.server.cache.WaterCacheQueue;
import lombok.extern.slf4j.Slf4j;
import store.StoreApi;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author: wz
 * @Date: 2021/10/22 09:55
 * @Description: 调度中心核心调度器
 */
@Slf4j
public abstract class AbsoluteTaskHandler implements TaskHandler {

    //获取调度头部信息
    protected abstract MetaHead getMetaHead();

    @Override
    public boolean handler() {
        MetaHead metaHead = getMetaHead();
        if (metaHead == null) return false;
        try {
            mainHandler(metaHead);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return true;
    }

    //待执行任务监听
    private void mainHandler(MetaHead head) {
        ReentrantLock lock = head.lock();
        try {
            if (!lock.tryLock(5000, TimeUnit.MILLISECONDS)) {
                return;
            }
            //判断缓存是否应该 加入新的数据并加入新的数据
            //处理缓冲队列的数据
            CacheHandler.cacheHandler(head);
            //开始调度分配任务
            if (trigger(head)) TaskDispatcher.getInstance().dispatch(head);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    //判断是否达到发送消息的条件
    //   触发条件: 1.超过指定时间 2.缓冲队列已经满
    private boolean trigger(MetaHead head) {
        ReentrantLock lock = head.lock();
        AtomicLong lastSendTime = head.lastSendTime();
        int maxWaitTime = head.maxWaitTime();
        WaterCacheQueue<DownloadTask> cacheQueue = head.cacheQueue();
        try {
            lock.lock();
            //超时触发
            long now = System.currentTimeMillis();
            if (now - lastSendTime.get() >= maxWaitTime) {
                resetSendTime(head);
                return true;
            }
            //高水位发送
            if (cacheQueue.isEnough()) {
                return true;
            }
        } finally {
            lock.unlock();
        }
        return false;
    }

    //重置发布时间
    private void resetSendTime(MetaHead head) {
        ReentrantLock lock = head.lock();
        try {
            lock.lock();
            head.lastSendTime().set(System.currentTimeMillis());
        } finally {
            lock.unlock();
        }
    }


    //掷色子决定谁的优先级更高(可以用随机数)
    //如果下载limit=8 重试limit=2  最坏情况(每次空闲1个) 5:5 最好情况 8:2
    private int throwDice(QueueInfo queue, int remain, int queueLimit, int retryLenLimit, int last) {
        //如果上次下载队列优先级高，这次重试队列优先级高
        if (last == 0) {
            //如果剩余空位大于 重试限制，优先重试队列
            // 例如  重试limit=2 下载limit=10 空余10 个位置  ==> 重试分2个 下载分8个
            if (remain >= retryLenLimit) {
                queue.retryCount(retryLenLimit);
                //剩下的位置都是正常任务的位置
                queue.remainCount(remain - retryLenLimit);
            } else {
                //如果剩余位置不够,优先重试队列
                //例如 重试limit=2 下载limit=10 空余1 个位置 ==> 重试分1个 下载分0个
                queue.retryCount(remain);
                queue.remainCount(0);
            }
            //返回这次优先的模式: 重试队列
            return 1;
        } else {
            //否则下载队列优先级高
            if (remain >= queueLimit) {
                //优先把下载队列填满
                queue.remainCount(queueLimit);
                //剩下的位置才是重试任务的
                queue.retryCount(remain - queueLimit);
            } else {
                //如果剩余位置不够,优先重试队列
                //例如 重试limit=2 下载limit=10 空余7 个位置 ==> 重试分0个 下载分7个
                queue.remainCount(remain);
                queue.retryCount(0);
            }
            //返回这次优先的模式: 下载队列
            return 0;
        }
    }
}
