package com.lingfeng.biz.downloader.netty;

import cn.hutool.extra.spring.SpringUtil;
import com.lingfeng.biz.downloader.model.DownloadTask;
import com.lingfeng.biz.downloader.model.TaskFrame;
import com.lingfeng.rpc.client.handler.AbsClientHandler;
import com.lingfeng.rpc.constant.Cmd;
import com.lingfeng.rpc.data.Frame;
import com.lingfeng.rpc.frame.SafeFrame;
import com.lingfeng.rpc.invoke.RpcInvokeProxy;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
public class NettyReqHandler extends AbsClientHandler<SafeFrame<TaskFrame<DownloadTask>>> {

    private volatile ThreadPoolTaskExecutor executor;

    public ThreadPoolTaskExecutor getExecutor() {
        if (executor == null) {
            synchronized (this) {
                if (executor == null) {
                    executor = SpringUtil.getBean("downloaderThreadPool");
                }
            }
        }
        return executor;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SafeFrame<TaskFrame<DownloadTask>> data) throws Exception {
        byte cmd = data.getCmd();
        if (cmd == Cmd.REQUEST.code()) {
            TaskFrame<DownloadTask> frame = data.getContent();
            DownloadTask data1 = frame.getData();
            String name = frame.getTarget();
            Channel channel = ctx.channel();
            //使用线程池处理任务
            getExecutor().execute(() -> {
                //代理执行方法
                RpcInvokeProxy.invoke(channel, ret -> {
                    // FinishNotify finishNotify;
                    // finishNotify.finish(666,"666");
                    //返回数据
                    Frame<Object> resp0 = new Frame<>();
                    TaskFrame<DownloadTask> resp = new TaskFrame<>();
                    resp.setData((DownloadTask) ret);
                    writeAndFlush(ctx.channel(), resp, Cmd.RESPONSE);
                }, name, frame.getData());
            });
        } else {
            ctx.fireChannelRead(data);
        }
    }
}
