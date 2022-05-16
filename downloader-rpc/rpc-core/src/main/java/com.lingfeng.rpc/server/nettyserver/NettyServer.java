package com.lingfeng.rpc.server.nettyserver;

import com.lingfeng.rpc.constant.Cmd;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.io.Serializable;
import java.util.Collection;

public interface NettyServer {

    int state();

    void start();

    void restart();

    void stop();

    long getServerId();

    <M extends Serializable> void writeAndFlush(Channel channel, M msg, Cmd type);

    <M extends Serializable> void writeAndFlush(ChannelHandlerContext channel, M msg, Cmd type);

    void addChannel(String clientId, Channel channel);

    void closeChannel(String clientId);

    Collection<Channel> allChannels();

    Channel findChanel(String clientId);

    void showChannels();

    void setDefaultChannelContext(ChannelHandlerContext channelContext);

    ChannelHandlerContext getDefaultChannelContext();

}
