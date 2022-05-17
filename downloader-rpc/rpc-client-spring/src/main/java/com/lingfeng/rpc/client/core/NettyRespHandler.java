package com.lingfeng.rpc.client.core;

import com.lingfeng.rpc.client.handler.AbsClientHandler;
import com.lingfeng.rpc.constant.Cmd;
import com.lingfeng.rpc.data.Frame;
import com.lingfeng.rpc.frame.SafeFrame;
import com.lingfeng.rpc.invoke.RpcInvokeProxy;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class NettyRespHandler extends AbsClientHandler<SafeFrame<Frame<?>>> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SafeFrame<Frame<?>> data) throws Exception {
        byte cmd = data.getCmd();
        if (cmd == Cmd.RESPONSE.code()) {
            Frame<?> frame = data.getContent();
            String name = frame.getTarget();
            //代理执行方法
            RpcInvokeProxy.invoke(ret -> {
                log.info("resp:{}", ret);
            }, name, frame.getData());
        } else {
            // ctx.fireChannelRead(data);
        }
    }
}
