package com.lingfeng.biz.downloader.task.multiplyhandler;


import com.lingfeng.biz.downloader.config.NodeConfig;
import com.lingfeng.biz.downloader.model.DTask;
import com.lingfeng.biz.downloader.task.downloader.AbstractDownloader;
import com.lingfeng.biz.downloader.task.downloader.Downloader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * @Author: wz
 * @Date: 2022/1/4 18:40
 * @Description: 多任务处理器
 */
@Slf4j
@Component
public class MultiplyTaskHandler extends AbstractMultiplyTaskPool<DTask> {
    //此处由于多个下载器是共享的 多任务处理器 所以在没有给各自分配 队列限制前，队列和锁也必须共享
    private final static ReentrantLock lock = new ReentrantLock();

    @Autowired
    private NodeConfig config;

    @Resource(name = "downloaderThreadPool")
    private ThreadPoolTaskExecutor executor;

    @Override
    protected ThreadPoolTaskExecutor getExecutor() {
        return executor;
    }

    @Override
    protected Lock getLock() {
        return lock;
    }


    @Override
    protected boolean doWork(DTask task) {
        if (task == null) {
            return false;
        }
        //选择下载器
        Downloader<DTask> downloader = AbstractDownloader.selectDownloader(task);
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

    @Override
    public int getLimit() {
        return config.getConcurrentLimit();
    }

}



