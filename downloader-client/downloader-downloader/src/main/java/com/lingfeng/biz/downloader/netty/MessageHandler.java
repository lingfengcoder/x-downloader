package com.lingfeng.biz.downloader.netty;

import com.lingfeng.biz.downloader.model.DownloadTask;
import com.lingfeng.biz.downloader.model.FileTask;
import com.lingfeng.biz.downloader.task.process.AsyncDownloadProcess;
import com.lingfeng.rpc.ann.RpcComponent;
import com.lingfeng.rpc.ann.RpcHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * @Author: wz
 * @Date: 2022/5/26 13:50
 * @Description:
 */
@Slf4j
@RpcComponent
public class MessageHandler {

    @Autowired
    //异步下载器
    private AsyncDownloadProcess asyncDownloadProcess;

    //获取任务，并进行处理
    @RpcHandler("listenTask")
    public Object listenTask(DownloadTask downloadTask) {
        Thread thread = Thread.currentThread();
        log.info("listenTask get msg {} {}", downloadTask, thread);
        FileTask fileTask = new FileTask();
        boolean isJoinQueue = asyncDownloadProcess.download(fileTask);
        return isJoinQueue ? "ack" : "nack";
    }
}
