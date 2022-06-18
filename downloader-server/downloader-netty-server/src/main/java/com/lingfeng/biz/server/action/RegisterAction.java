package com.lingfeng.biz.server.action;

import com.lingfeng.biz.downloader.model.BasicCmd;
import com.lingfeng.biz.downloader.model.BasicFrame;
import com.lingfeng.rpc.ann.RpcComponent;
import com.lingfeng.rpc.ann.RpcHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * @Auther: wz
 * @Date: 2022/6/16 17:30
 * @Description:
 */
@Slf4j
@RpcComponent
public class RegisterAction {

    @RpcHandler("register")
    public void register(BasicFrame<Object> frame) {
        log.info("register={}", frame);
    }
}
