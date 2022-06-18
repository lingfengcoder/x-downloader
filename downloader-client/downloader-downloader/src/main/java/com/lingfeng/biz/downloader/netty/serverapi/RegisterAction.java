package com.lingfeng.biz.downloader.netty.serverapi;

import com.lingfeng.biz.downloader.model.BasicFrame;
import com.lingfeng.rpc.ann.RpcClient;

/**
 * @Auther: wz
 * @Date: 2022/6/16 16:16
 * @Description:
 */
@RpcClient
public interface RegisterAction {
    void register(BasicFrame<Object> frame);
}
