package com.lingfeng.rpc.client.nettyclient;

import com.lingfeng.rpc.constant.Cmd;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public interface NettyClient {

    int state();

    void config(Consumer<NettyClient> consumer);

    void start();

    //totalTime:ms 总时间内 perTime+unit=重试频率
    void restart(long totalTime, TimeUnit unit, long perTime);

    void restart();

    void close();

    long getClientId();

    void defaultChannel(Channel channel);

    NettyClient addHandler(ChannelHandler handler, String name);

    <F extends Future<?>> NettyClient addListener(GenericFutureListener<F> listener);

    <M extends Serializable> void writeAndFlush(Channel channel, M msg, Cmd type);
//     保留接口 <M extends Serializable> void writeAndFlush(M msg, Cmd type);
}
