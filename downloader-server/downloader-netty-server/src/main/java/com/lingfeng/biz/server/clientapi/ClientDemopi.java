package com.lingfeng.biz.server.clientapi;


import com.lingfeng.biz.downloader.model.BasicFrame;
import com.lingfeng.biz.downloader.model.DownloadTask;
import com.lingfeng.rpc.ann.RpcClient;

@RpcClient("rpcDemoHandler")
public interface ClientDemopi {
    //获取任务，并进行处理
    Object clientTest(BasicFrame<?> frame);

    Object testDownloadTask(DownloadTask frame);

}
