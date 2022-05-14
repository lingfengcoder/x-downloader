package com.lingfeng.rpc.client.core;

import com.lingfeng.rpc.client.handler.AbsClientHandler;
import com.lingfeng.rpc.constant.Cmd;
import com.lingfeng.rpc.data.Frame;
import com.lingfeng.rpc.frame.SafeFrame;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class NettyReqHandler extends AbsClientHandler<SafeFrame<Frame<?>>> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SafeFrame<Frame<?>> data) throws Exception {
        byte cmd = data.getCmd();
        if (cmd == Cmd.REQUEST.code()) {
            Frame<?> frame = data.getContent();
            String name = frame.getTarget();
            //代理执行方法
            RpcInvokeProxy.invoke(ret -> {
                //返回数据
                Frame<Object> resp = new Frame<>();
                resp.setData(ret);
                writeAndFlush(ctx.channel(), resp, Cmd.RESPONSE);

            }, name, frame.getData());
        } else {
            ctx.fireChannelRead(data);
        }
    }
}
