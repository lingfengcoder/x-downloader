package com.lingfeng.biz.server.handler;


import com.lingfeng.biz.downloader.model.TaskFrame;
import com.lingfeng.rpc.constant.Cmd;
import com.lingfeng.rpc.data.Frame;
import com.lingfeng.rpc.frame.SafeFrame;
import com.lingfeng.rpc.server.handler.AbsServerHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
@ChannelHandler.Sharable
public class BizMessageServerHandler extends AbsServerHandler<SafeFrame<TaskFrame<?>>> {

    @Resource(name = "dispatcherSenderThreadPool")
    private ThreadPoolTaskExecutor dispatcherSenderThreadPool;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SafeFrame<TaskFrame<?>> data) throws Exception {
        byte cmd = data.getCmd();
        // request
        if (cmd == Cmd.REQUEST.code()) {
            TaskFrame<?> frame = data.getContent();
            log.info(" downloader server get REQUEST data = {}", frame);
            //返回数据
            // writeAndFlush(ctx.channel(), resp, Cmd.REQUEST);
            dispatcherSenderThreadPool.execute();
        }
        //response
        if (cmd == Cmd.RESPONSE.code()) {
            Frame<?> frame = data.getContent();
            log.info("downloader server get RESPONSE data = {}", frame);
            dispatcherSenderThreadPool.execute();
        } else {
            //ctx.fireChannelRead(data);
        }
    }
}
