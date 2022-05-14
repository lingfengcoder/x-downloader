package com.lingfeng.rpc.client.nettyclient;

import com.lingfeng.rpc.constant.Cmd;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public interface NettyClient {

    int state();

    void start();

    void restart();

    void close();

    long getClientId();

    void defaultChannel(Channel channel);


    <M extends Serializable> void writeAndFlush(Channel channel, M msg, Cmd type);

    <M extends Serializable> void writeAndFlush(ChannelHandlerContext channel, M msg, Cmd type);
//     保留接口 <M extends Serializable> void writeAndFlush(M msg, Cmd type);
}
