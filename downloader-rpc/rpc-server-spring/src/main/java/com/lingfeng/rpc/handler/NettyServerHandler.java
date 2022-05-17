package com.lingfeng.rpc.handler;


import com.lingfeng.rpc.constant.Cmd;


import com.lingfeng.rpc.data.Frame;
import com.lingfeng.rpc.frame.SafeFrame;
import com.lingfeng.rpc.server.handler.AbsServerHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NettyServerHandler extends AbsServerHandler<SafeFrame<Frame<?>>> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SafeFrame<Frame<?>> data) throws Exception {
        byte cmd = data.getCmd();
        // request
        if (cmd == Cmd.REQUEST.code()) {
            Frame<?> frame = data.getContent();
            log.info("  server get REQUEST data = {}", frame);
            //返回数据
            // writeAndFlush(ctx.channel(), resp, Cmd.REQUEST);
        }
        //response
        if (cmd == Cmd.RESPONSE.code()) {
            Frame<?> frame = data.getContent();
            log.info("server get RESPONSE data = {}", frame);
        } else {
            //ctx.fireChannelRead(data);
        }
    }
}
