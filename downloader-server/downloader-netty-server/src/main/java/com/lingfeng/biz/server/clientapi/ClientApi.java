package com.lingfeng.biz.server.clientapi;


import com.lingfeng.biz.downloader.model.DownloadTask;
import com.lingfeng.rpc.ann.RpcClient;

@RpcClient("messageHandler")
public interface ClientApi {
    //获取任务，并进行处理
    Object listenTask(DownloadTask downloadTask);
}
