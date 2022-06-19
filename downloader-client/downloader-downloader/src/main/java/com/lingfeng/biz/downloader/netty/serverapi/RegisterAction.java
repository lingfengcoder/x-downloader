package com.lingfeng.biz.downloader.netty.serverapi;

import com.lingfeng.biz.downloader.model.BasicFrame;
import com.lingfeng.rpc.ann.RpcClient;

import javax.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Auther: wz
 * @Date: 2022/6/16 16:16
 * @Description:
 */
@RpcClient("registerAction")
public interface RegisterAction {
    void register(BasicFrame<Object> frame);

    void test(ConcurrentHashMap<String, Integer> map);
}
