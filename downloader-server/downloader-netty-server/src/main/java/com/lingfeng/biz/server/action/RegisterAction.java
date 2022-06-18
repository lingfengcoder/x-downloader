package com.lingfeng.biz.server.action;

import com.lingfeng.biz.downloader.model.BasicCmd;
import com.lingfeng.biz.downloader.model.BasicFrame;
import com.lingfeng.biz.server.client.NodeClientGroup;
import com.lingfeng.biz.server.model.NodeClient;
import com.lingfeng.rpc.ann.RpcComponent;
import com.lingfeng.rpc.ann.RpcHandler;
import com.lingfeng.rpc.invoke.RpcInvokeProxy;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.lang.ref.WeakReference;

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
        log.info("server get a register req!");
        log.info("server get a register req!");
        log.info("server get a register req!");
        BasicCmd cmd = frame.getCmd();
        NodeClientGroup clientStore = NodeClientGroup.getInstance();
        Channel channel = RpcInvokeProxy.getChannel();
        switch (cmd) {
            //如果是注册帧 注册客户端
            case REG:
                NodeClient client = NodeClient.builder()
                        .alive(true)//激活状
                        .channelId(channel.id().asLongText())//channel id
                        .channel(new WeakReference<>(channel))//channel弱引用
                        .clientId(frame.getClientId())//注册的客户端的id
                        .modifyTime(System.currentTimeMillis())//注册时间
                        .build();
                clientStore.addNodeClient(client);
                break;
            //如果是关闭帧，关闭客户端
            case CLOSE:
                String clientId = frame.getClientId();
                //todo 主动关闭channel
                clientStore.removeNodeClient(clientId);
                break;
        }
    }
}
