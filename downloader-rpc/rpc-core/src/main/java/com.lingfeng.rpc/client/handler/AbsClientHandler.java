package com.lingfeng.rpc.client.handler;

import com.lingfeng.rpc.client.nettyclient.NettyClient;
import com.lingfeng.rpc.constant.Cmd;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

/**
 * @Author: wz
 * @Date: 2022/5/11 14:16
 * @Description:
 */
@Slf4j
public abstract class AbsClientHandler<T> extends SimpleChannelInboundHandler<T> {

    private volatile NettyClient client;

    public NettyClient getClient() {
        return client;
    }

    public AbsClientHandler<T> setClient(NettyClient client) {
        this.client = client;
        return this;
    }

    public long getClientId() {
        return client.getClientId();
    }

    @Override
    //管道被激活的时候
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    public <M extends Serializable> void writeAndFlush(ChannelHandlerContext channel, M msg, Cmd type) {
        getClient().writeAndFlush(channel, msg, type);
    }

    public <M extends Serializable> void writeAndFlush(Channel channel, M msg, Cmd type) {
        getClient().writeAndFlush(channel, msg, type);
    }


    protected ByteBuf buildMsg(String msg) {
//        CompositeByteBuf byteBufs = Unpooled.compositeBuffer();
        return Unpooled.copiedBuffer(msg, CharsetUtil.UTF_8);
    }

    protected String parseStr(ByteBuf byteBuf) {
        return byteBuf.toString(CharsetUtil.UTF_8);
    }

}
