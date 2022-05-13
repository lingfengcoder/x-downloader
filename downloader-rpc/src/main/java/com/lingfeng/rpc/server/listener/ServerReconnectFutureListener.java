package com.lingfeng.rpc.server.listener;

import com.lingfeng.rpc.server.nettyserver.NettyServer;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoop;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j

//重连-监听器
public class ServerReconnectFutureListener implements ChannelFutureListener {

    private volatile NettyServer server;

    @Override
    public void operationComplete(ChannelFuture channelFuture) throws Exception {
        if (channelFuture.isSuccess()) {
            long clientId = getServer().getServerId();
            log.info("[netty server id: {}].isSuccess  ", clientId);
            return;
        }
        final EventLoop loop = channelFuture.channel().eventLoop();
        loop.schedule(() -> {
            long clientId = getServer().getServerId();
            try {
                log.info("[netty server id: {}]  失败重连", clientId);

                log.info("itstack-demo-netty server start done.  ");
                Thread.sleep(500);
            } catch (Exception e) {
                log.info("[netty server id: {}] start error go reconnect ...{}  ", clientId, e.getMessage(), e);
            }
        }, 1L, TimeUnit.SECONDS);
    }

    public NettyServer getServer() {
        return server;
    }

    public void setServer(NettyServer server) {
        this.server = server;
    }
}
