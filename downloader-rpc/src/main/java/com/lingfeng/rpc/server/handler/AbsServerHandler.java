package com.lingfeng.rpc.server.handler;

import com.lingfeng.rpc.constant.Cmd;
import com.lingfeng.rpc.server.nettyserver.BizNettyServer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
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
    private volatile BizNettyServer server;
    private volatile ChannelHandlerContext channel;
    //连接通道的集合
    private final ConcurrentHashMap<Long, Channel> channels = new ConcurrentHashMap<>();

    public void closeChannel(Long channelId) {
        Channel channel = channels.get(channelId);
        if (channel != null) {
            log.info("关闭channel{}", channelId);
            channel.close();
            channels.remove(channelId);
        }
    }

    public void addChannel(Long clientId, Channel channel) {
        // InetSocketAddress socketAddress = (InetSocketAddress) channel.remoteAddress();
        if (!channels.containsKey(clientId)) {
            channels.put(clientId, channel);
        }
    }

    public long getServerId() {
        return server.getServerId();
    }

    @Override
    //管道被激活的时候
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.setChannel(ctx);
        super.channelActive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        log.error("[netty server handler serverId={}]  exceptionCaught = {} ", getServerId(), cause.getMessage(), cause);
        //发生异常，关闭通道
        ctx.close();
    }

    public BizNettyServer getServer() {
        return server;
    }

    public void setServer(BizNettyServer server) {
        this.server = server;
    }

    public ChannelHandlerContext getChannel() {
        return channel;
    }

    public void setChannel(ChannelHandlerContext channel) {
        this.channel = channel;
    }

    public ConcurrentHashMap<Long, Channel> getChannels() {
        return channels;
    }

    public <M extends Serializable> void writeAndFlush(Channel channel, M msg, Cmd type) {
        getServer().writeAndFlush(channel, msg, type);
    }
}
