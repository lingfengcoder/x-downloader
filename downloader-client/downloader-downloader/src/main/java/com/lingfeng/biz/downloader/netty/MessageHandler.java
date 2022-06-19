package com.lingfeng.biz.downloader.netty;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import com.lingfeng.biz.downloader.model.DownloadTask;
import com.lingfeng.biz.downloader.model.FileTask;
import com.lingfeng.biz.downloader.task.process.AsyncDownloadProcess;
import com.lingfeng.biz.downloader.util.UrlParser;
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
@RpcComponent("messageHandler")
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
        fileTask.setSourceUrl(downloadTask.getUrl());
        fileTask.setFileLocalPath("/data/x-downloader/file/" + UrlParser.parseFileName(downloadTask.getUrl()));
        fileTask.setFileType(1);
        fileTask.setFileCode(RandomUtil.randomString(8));
        fileTask.setAsync(1);
        fileTask.setNotifyUrl("http://localhost:7002/api/testCallback");
        boolean isJoinQueue = asyncDownloadProcess.download(fileTask);
        return isJoinQueue ? "ack" : "nack";
    }


}
