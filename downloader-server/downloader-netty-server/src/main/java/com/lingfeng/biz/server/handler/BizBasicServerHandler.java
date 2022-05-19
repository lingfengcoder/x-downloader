package com.lingfeng.biz.server.handler;


import com.lingfeng.biz.downloader.model.BasicCmd;
import com.lingfeng.biz.downloader.model.BasicFrame;
import com.lingfeng.biz.downloader.model.DownloadTask;
import com.lingfeng.biz.downloader.model.TaskFrame;
import com.lingfeng.biz.server.dispatcher.NodeClientStore;
import com.lingfeng.biz.server.model.NodeClient;
import com.lingfeng.rpc.constant.Cmd;
import com.lingfeng.rpc.frame.SafeFrame;
import com.lingfeng.rpc.server.handler.AbsServerHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.ref.WeakReference;

@Slf4j
@Component
@ChannelHandler.Sharable //共享的处理器 因为是默认单例
//基础处理器 主要负责 管理客户端
public class BizBasicServerHandler extends AbsServerHandler<SafeFrame<BasicFrame<Object>>> {
    @Autowired
    private NodeClientStore clientStore;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SafeFrame<BasicFrame<Object>> data) {
        byte cmd = data.getCmd();
        // request 主要处理client的请求，比如 客户端主动请求获取最新的配置 客户端注册
        if (cmd == Cmd.REQUEST.code()) {
            BasicFrame<Object> frame = data.getContent();
            log.info(" REQUEST data = {}", frame);
            frameHandler(ctx, frame);
        } else ctx.fireChannelRead(data);
    }

    private void frameHandler(ChannelHandlerContext ctx, BasicFrame<Object> frame) {
        BasicCmd cmd = frame.getCmd();
        switch (cmd) {
            //如果是注册帧 注册客户端
            case REG:
                NodeClient client = NodeClient.builder()
                        .alive(true)//激活状
                        .channel(new WeakReference<>(ctx.channel()))//channel弱引用
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
