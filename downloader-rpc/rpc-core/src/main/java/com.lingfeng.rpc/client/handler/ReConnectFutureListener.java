package com.lingfeng.rpc.client.handler;

import com.lingfeng.rpc.client.nettyclient.NettyClient;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoop;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j

//重连-监听器
public class ReConnectFutureListener implements ChannelFutureListener {

    private volatile NettyClient client;


    @Override
    public void operationComplete(ChannelFuture channelFuture) throws Exception {
        log.info("client operationComplete===============");
        if (channelFuture.isSuccess()) {
            long clientId = getClient().getClientId();
            log.info("[netty client id: {}].isSuccess  ", clientId);
            return;
        } else {
        }
        final EventLoop loop = channelFuture.channel().eventLoop();
        loop.schedule(() -> {
            long clientId = getClient().getClientId();
            try {
                log.info("[netty client id: {}]  失败重连", clientId);

                log.info("itstack-demo-netty client start done.  ");
                Thread.sleep(500);
            } catch (Exception e) {
                log.info("[netty client id: {}] start error go reconnect ...{}  ", clientId, e.getMessage(), e);
            }
        }, 1L, TimeUnit.SECONDS);
    }

    public NettyClient getClient() {
        return client;
    }

    public void setClient(NettyClient client) {
        this.client = client;
    }
}
