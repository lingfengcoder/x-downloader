package com.lingfeng.rpc;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: wz
 * @Date: 2022/5/7 18:29
 * @Description:
 */
@Slf4j
public class MyClientHandler extends ChannelInboundHandlerAdapter {
    private final static MyClientHandler clientHandler = new MyClientHandler();

    public static MyClientHandler getInstance() {
        return clientHandler;
    }

    private volatile ChannelHandlerContext channel;

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        if (channel == null) {
            channel = ctx;
            log.info("channel 客户端注册");
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //发送消息到服务端
        ctx.writeAndFlush(Unpooled.copiedBuffer("歪比巴卜~茉莉~Are you good~马来西亚~", CharsetUtil.UTF_8));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //接收服务端发送过来的消息
        ByteBuf byteBuf = (ByteBuf) msg;
        log.info("收到服务端" + ctx.channel().remoteAddress() + "的消息：" + byteBuf.toString(CharsetUtil.UTF_8));
    }


    public void write(String msg) {
        while (channel == null) {
            log.info("channel == null");
        }
        channel.writeAndFlush(Unpooled.copiedBuffer(msg, CharsetUtil.UTF_8));
    }

}
