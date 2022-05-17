package com.lingfeng.rpc.client.core;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import cn.hutool.extra.spring.SpringUtil;
import com.lingfeng.rpc.client.handler.AbsClientHandler;
import com.lingfeng.rpc.constant.Cmd;
import com.lingfeng.rpc.data.Frame;
import com.lingfeng.rpc.frame.SafeFrame;
import com.lingfeng.rpc.invoke.RpcInvokeProxy;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
public class NettyReqHandler extends AbsClientHandler<SafeFrame<Frame<?>>> {

    private volatile ThreadPoolTaskExecutor executor;

    public ThreadPoolTaskExecutor getExecutor() {
        if (executor == null) {
            synchronized (this) {
                if (executor == null) {
                    executor = SpringUtil.getBean("dispatcherThreadPool");
                }
            }
        }
        return executor;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SafeFrame<Frame<?>> data) throws Exception {
        byte cmd = data.getCmd();
        if (cmd == Cmd.REQUEST.code()) {
            Frame<?> frame = data.getContent();
            String name = frame.getTarget();
            //使用线程池处理任务
            getExecutor().execute(() -> {
                //代理执行方法
                RpcInvokeProxy.invoke(ret -> {
                    //返回数据
                    Frame<Object> resp = new Frame<>();
                    resp.setData(ret);
                    writeAndFlush(ctx.channel(), resp, Cmd.RESPONSE);

                }, name, frame.getData());
            });

        } else {
           // ctx.fireChannelRead(data);
        }
    }
}
