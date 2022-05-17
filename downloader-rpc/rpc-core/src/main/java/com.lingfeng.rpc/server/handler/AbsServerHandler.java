package com.lingfeng.rpc.server.handler;

import com.lingfeng.rpc.constant.Cmd;
import com.lingfeng.rpc.server.nettyserver.BizNettyServer;
import com.lingfeng.rpc.server.nettyserver.NettyServer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: wz
 * @Date: 2022/5/11 14:16
 * @Description:
 */
@Slf4j
public abstract class AbsServerHandler<T> extends SimpleChannelInboundHandler<T> {
    private volatile NettyServer server;
    private volatile ChannelHandlerContext channel;


    public long getServerId() {
        return server.getServerId();
    }

    @Override
    //管道被激活的时候
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.setChannel(ctx);
        NettyServer server = getServer();
        server.setDefaultChannelContext(ctx);
        super.channelActive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        log.error("[netty server handler serverId={}]  exceptionCaught = {} ", getServerId(), cause.getMessage(), cause);
        //发生异常，关闭通道
        ctx.close();
    }

    public NettyServer getServer() {
        return server;
    }

    public void setServer(NettyServer server) {
        this.server = server;
    }

    public ChannelHandlerContext getChannel() {
        return channel;
    }

    public void setChannel(ChannelHandlerContext channel) {
        this.channel = channel;
    }

    public <M extends Serializable> void writeAndFlush(Channel channel, M msg, Cmd type) {
        getServer().writeAndFlush(channel, msg, type);
    }

    protected ByteBuf buildMsg(String msg) {
//        CompositeByteBuf byteBufs = Unpooled.compositeBuffer();
        return Unpooled.copiedBuffer(msg, CharsetUtil.UTF_8);
    }

    protected String parseStr(ByteBuf byteBuf) {
        return byteBuf.toString(CharsetUtil.UTF_8);
    }
}
