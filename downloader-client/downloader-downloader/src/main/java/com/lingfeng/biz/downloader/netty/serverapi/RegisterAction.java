package com.lingfeng.biz.downloader.netty.serverapi;

import com.lingfeng.biz.downloader.model.BasicFrame;
import com.lingfeng.rpc.ann.RpcClient;

import javax.annotation.PostConstruct;

/**
 * @Auther: wz
 * @Date: 2022/6/16 16:16
 * @Description:
 */
@RpcClient("testRpcClient")
public interface RegisterAction {
    void register(BasicFrame<Object> frame);
}
