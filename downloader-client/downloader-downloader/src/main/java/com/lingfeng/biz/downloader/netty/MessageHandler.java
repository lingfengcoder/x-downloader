package com.lingfeng.biz.downloader.netty;

import com.lingfeng.biz.downloader.model.DownloadTask;
import com.lingfeng.rpc.ann.RpcComponent;
import com.lingfeng.rpc.ann.RpcHandler;
import lombok.extern.slf4j.Slf4j;


/**
 * @Author: wz
 * @Date: 2022/5/26 13:50
 * @Description:
 */
@Slf4j
@RpcComponent
public class MessageHandler {

    @RpcHandler("listenTask")
    public Object listenTask(DownloadTask task) {
        Thread thread = Thread.currentThread();
        log.info("listenTask get msg {}", task);
        return " get msg ";
    }
}
