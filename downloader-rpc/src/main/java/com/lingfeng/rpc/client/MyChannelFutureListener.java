package com.lingfeng.rpc.client;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoop;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
@Setter
@Accessors(chain = true)
public class MyChannelFutureListener implements ChannelFutureListener {
    private volatile int clientId;
    private volatile NettyClient client;

    @Override
    public void operationComplete(ChannelFuture channelFuture) throws Exception {
        if (channelFuture.isSuccess()) {
            log.info("[netty client id: {}].isSuccess  ", clientId);
            return;
        }
        final EventLoop loop = channelFuture.channel().eventLoop();
        loop.schedule(() -> {
            try {
                log.info("[netty client id: {}]  失败重连", clientId);
                //new NettyClient().connect("127.0.0.1", 7397);
                log.info("itstack-demo-netty client start done.  ");
                Thread.sleep(500);
            } catch (Exception e) {
                log.info("[netty client id: {}] start error go reconnect ...{}  ", clientId, e.getMessage(), e);
            }
        }, 1L, TimeUnit.SECONDS);
    }
}
