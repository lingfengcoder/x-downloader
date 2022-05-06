package com.pukka.iptv.downloader.task.multiplyhandler;

import cn.hutool.extra.spring.SpringUtil;
import com.pukka.iptv.downloader.config.NodeConfig;
import com.pukka.iptv.downloader.model.DownloadTask;
import com.pukka.iptv.downloader.task.downloader.AbstractDownloader;
import com.pukka.iptv.downloader.task.downloader.Downloader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * @Author: wz
 * @Date: 2022/1/4 18:40
 * @Description: 多任务处理器
 */
@Slf4j
@Component
public class MultiplyTaskHandler extends AbstractMultiplyTaskPool<DownloadTask> {
    //此处由于多个下载器是共享的 多任务处理器 所以在没有给各自分配 队列限制前，队列和锁也必须共享
    private final static ReentrantLock lock = new ReentrantLock();
    private final static PriorityQueue<DownloadTask> waitQueue = new PriorityQueue<>();
    private final static PriorityQueue<DownloadTask> workingQueue = new PriorityQueue<>();
    @Autowired
    private NodeConfig config;

    @Override
    protected ExecutorService getExecutor() {
        return SpringUtil.getBean("downloaderThreadPool");
    }

    @Override
    protected int waitQueueLimit() {
        return config.getConcurrentLimit();
    }

    @Override
    protected int workQueueLimit() {
        return NO_LIMIT;
    }

    @Override
    protected Lock getLock() {
        return lock;
    }

    @Override
    protected PriorityQueue<DownloadTask> getWaitQueue() {
        return waitQueue;
    }

    @Override
    protected PriorityQueue<DownloadTask> getWorkingQueue() {
        return workingQueue;
    }

    @Override
    protected boolean doWork(DownloadTask task) {
        if (task == null) {
            return false;
        }
        //选择下载器
        Downloader<DownloadTask> downloader = AbstractDownloader.selectDownloader(task);
        assert downloader != null;
        downloader.preHandler(task);
        try {
            downloader.download(task);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        downloader.postHandler(task);
        return true;
    }


}



