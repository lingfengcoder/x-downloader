package com.lingfeng.biz.server.handler;


import com.lingfeng.biz.downloader.model.DownloadTask;
import com.lingfeng.biz.downloader.model.TaskFrame;
import com.lingfeng.biz.server.dispatcher.NodeClientStore;
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

@Slf4j
@Component
@ChannelHandler.Sharable //共享的处理器 因为是默认单例
public class BizMessageServerHandler extends AbsServerHandler<SafeFrame<TaskFrame<DownloadTask>>> {

    @Resource(name = "dispatcherSenderThreadPool")
    private ThreadPoolTaskExecutor dispatcherSenderThreadPool;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SafeFrame<TaskFrame<DownloadTask>> data) throws Exception {
        byte cmd = data.getCmd();
        // request 主要处理client的请求，比如 客户端主动请求获取最新的配置 客户端注册
        //response 主要处理client的数据返回，例如：任务的完成、任务的拒绝等等
        if (cmd == Cmd.RESPONSE.code()) {
            TaskFrame<DownloadTask> frame = data.getContent();
            log.info(" RESPONSE data = {}", frame);
            //提交给线程池执行
            TaskHandler handler = TaskHandler.builder().taskFrame(frame).build();
            dispatcherSenderThreadPool.execute(handler);
        } else ctx.fireChannelRead(data);
    }

}
