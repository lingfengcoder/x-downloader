package com.lingfeng.biz.server.handler;


import com.lingfeng.biz.downloader.model.BasicCmd;
import com.lingfeng.biz.downloader.model.BasicFrame;
import com.lingfeng.biz.server.dispatcher.NodeClientGroup;
import com.lingfeng.biz.server.model.NodeClient;
import com.lingfeng.rpc.constant.Cmd;
import com.lingfeng.rpc.frame.SafeFrame;
import com.lingfeng.rpc.server.handler.AbsServerHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.ref.WeakReference;
import java.util.Collection;

@Slf4j
@Component
@ChannelHandler.Sharable //共享的处理器 因为是默认单例
//基础处理器 主要负责 管理客户端
public class BizBasicServerHandler extends AbsServerHandler<SafeFrame<BasicFrame<Object>>> {


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

    @Override
    //客户端断开连接
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String channelId = ctx.channel().id().asLongText();
        NodeClientGroup clientStore = NodeClientGroup.getInstance();
        NodeClient client = clientStore.getClientByChannelId(channelId);
        //设置关闭状态
        client.setAlive(false);
        client.setModifyTime(System.currentTimeMillis());
        //剔除客户端 //todo 由后台线程去剔除超时未上线的客户端，并且处理路由器中的任务，是否可以将任务再次分配
        // clientStore.removeNodeClientByChannelId(channelId);
        super.channelInactive(ctx);
    }

    private void frameHandler(ChannelHandlerContext ctx, BasicFrame<Object> frame) {
        BasicCmd cmd = frame.getCmd();
        NodeClientGroup clientStore = NodeClientGroup.getInstance();
        switch (cmd) {
            //如果是注册帧 注册客户端
            case REG:
                Channel channel = ctx.channel();
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
