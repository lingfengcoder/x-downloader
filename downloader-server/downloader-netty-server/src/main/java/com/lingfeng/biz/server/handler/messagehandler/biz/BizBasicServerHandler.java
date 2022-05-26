package com.lingfeng.biz.server.handler.messagehandler.biz;


import com.lingfeng.biz.downloader.model.BasicFrame;
import com.lingfeng.biz.server.client.NodeClientGroup;
import com.lingfeng.biz.server.handler.messagehandler.BasicFrameHandler;
import com.lingfeng.biz.server.model.NodeClient;
import com.lingfeng.rpc.constant.Cmd;
import com.lingfeng.rpc.frame.SafeFrame;
import com.lingfeng.rpc.server.handler.AbsServerHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
@ChannelHandler.Sharable //共享的处理器 因为是默认单例
//基础处理器 主要负责 管理客户端
public class BizBasicServerHandler extends AbsServerHandler<SafeFrame<BasicFrame<Object>>> {

    @Resource(name = "dispatcherSenderThreadPool")
    private ThreadPoolTaskExecutor dispatcherSenderThreadPool;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SafeFrame<BasicFrame<Object>> data) {
        byte cmd = data.getCmd();
        // request 主要处理client的请求，比如 客户端主动请求获取最新的配置 客户端注册
        if (cmd == Cmd.REQUEST.code()) {
            BasicFrame<Object> frame = data.getContent();
            log.info(" REQUEST data = {}", frame);
            //线程池执行
            BasicFrameHandler handler = BasicFrameHandler.builder().channel(ctx.channel()).frame(frame).build();
            dispatcherSenderThreadPool.execute(handler);
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


}
