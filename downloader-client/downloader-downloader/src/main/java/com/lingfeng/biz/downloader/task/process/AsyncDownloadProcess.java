package com.lingfeng.biz.downloader.task.process;


import com.lingfeng.biz.downloader.model.*;
import com.lingfeng.biz.downloader.task.downloader.AbstractDownloader;
import com.lingfeng.biz.downloader.task.multiplyhandler.MultiplyTaskHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @Auther: wz
 * @Date: 2021/10/15 11:04
 * @Description: 异步下载任务执行处理器
 */
@Slf4j
@Component
public class AsyncDownloadProcess {
    @Autowired
    private MultiplyTaskHandler taskHandler;

    public boolean download(FileTask fileTask) {
        //前置简单处理
        downloadPreProcess(fileTask);
        //下载处理器
        return downloadProcess(fileTask);
    }

    //任务前置处理
    private void downloadPreProcess(FileTask task) {
        //重试次数+1
        task.setRetryCount(task.getRetryCount() + 1);
        //剔除地址中的文件名
        // task.setTargetFTPUrl(filterFilename(task.getTargetFTPUrl()));
        //获取当前节点的nacos唯一id
        String instanceId = null;//note listener.getDNode().getInstanceId();
        if (instanceId != null) {
            //设置当前处理消息的节点 (路由key)
            task.setTaskServerInstanceId(instanceId);
        } else {
            //note listener.nackTask(msgTask);
        }
    }


    //下载任务处理器
    private boolean downloadProcess(FileTask fileTask) {
        //创建下载任务
        DownloadTask task = AbstractDownloader.generalTask(fileTask);
        //任务执行完毕的 对消息进行ack的回调方法
        //note task.setCallback(t -> listener.ackTask(msgTask));
        //设置后置处理器
        //添加下载任务
        if (!taskHandler.submitTask(task)) {
            //添加失败，退回任务
            //todo 考虑 通知调度器 任务已满 nack
            return false;
        } else {
            //todo 通知调度器任务添加成功 ack
            return true;
        }
    }

    //获取每个下载器节点的任务数量
    public int getAllDownloaderQueueLen() {
        //out 获取所有 内置的下载器
        //由于队列是共享的，所以找其中一个下载器即可
        return taskHandler.getAliveTaskCount();
    }

}
