package com.lingfeng.rpc.server.nettyserver;

import com.lingfeng.rpc.constant.Cmd;
import io.netty.channel.Channel;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public interface NettyServer {

    int state();


    void start();

    void restart();

    void stop();

    long getServerId();

    <M extends Serializable> void writeAndFlush(Channel channel, M msg, Cmd type);

    void addChannel(String clientId, Channel channel);

    Channel findChanel(String clientId);

    void closeChannel(String clientId);

    void showChannels();

    Collection<Channel> allChannels();

}
