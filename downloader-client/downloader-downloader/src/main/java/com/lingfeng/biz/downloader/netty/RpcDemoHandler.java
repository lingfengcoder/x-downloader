package com.lingfeng.biz.downloader.netty;


import com.lingfeng.biz.downloader.model.BasicFrame;
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
@RpcComponent("rpcDemoHandler")
public class RpcDemoHandler {


    //获取任务，并进行处理
    @RpcHandler("clientTest")
    public Object clientTest(BasicFrame<?> frame) {
        log.info("clientTest get msg={}", frame);
        log.info("clientTest get msg={}", frame);
        log.info("clientTest get msg={}", frame);
        return "clientTest";
    }

    @RpcHandler("testDownloadTask")
    public Object testDownloadTask(DownloadTask frame) throws InterruptedException {
        log.info("testDownloadTask get DownloadTask={}", frame);
        log.info("testDownloadTask get DownloadTask={}", frame);
        log.info("testDownloadTask get DownloadTask={}", frame);
        return "clientTest";
    }
}
