package com.lingfeng.rpc.server.nettyserver;


import com.lingfeng.rpc.constant.Cmd;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.io.Serializable;

public interface NettyServer {

    int state();

    void start();

    void restart();

    void stop();

    NettyServer addHandler(ChannelHandler handler, String name);

    <F extends Future<?>> NettyServer addListener(GenericFutureListener<F> listener);

    <M extends Serializable> void writeAndFlush(Channel channel, M msg, Cmd type);
}
