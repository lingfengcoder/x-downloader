package com.lingfeng.biz.server.handler.messagehandler;

import com.lingfeng.biz.downloader.model.BasicCmd;
import com.lingfeng.biz.downloader.model.BasicFrame;
import com.lingfeng.biz.server.client.NodeClientGroup;
import com.lingfeng.biz.server.model.NodeClient;
import io.netty.channel.Channel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.lang.ref.WeakReference;

/**
 * @Author: wz
 * @Date: 2022/5/26 09:20
 * @Description: 基础帧的处理器
 */
@Getter
@Setter
@Builder
public class BasicFrameHandler implements Runnable {
    private Channel channel;
    private BasicFrame<Object> frame;

    @Override
    public void run() {
        BasicCmd cmd = frame.getCmd();
        NodeClientGroup clientStore = NodeClientGroup.getInstance();
        switch (cmd) {
            //如果是注册帧 注册客户端
            case REG:
                NodeClient client = NodeClient.builder()
                        .alive(true)//激活状
                        .channelId(channel.id().asLongText())//channel id
                        .channel(new WeakReference<>(channel))//channel弱引用
                        .clientId(frame.getClientId())//注册的客户端的id
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
