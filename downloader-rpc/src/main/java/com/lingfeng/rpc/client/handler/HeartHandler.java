package com.lingfeng.rpc.client.handler;


import com.lingfeng.rpc.client.nettyclient.NettyClient;
import com.lingfeng.rpc.constant.Cmd;
import com.lingfeng.rpc.frame.SafeFrame;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;


@Slf4j
//@ChannelHandler.Sharable
public class HeartHandler extends AbsClientHandler<SafeFrame<String>> {

    public final static String NAME = "idleTimeoutHandler";

    // 定义客户端没有收到服务端的pong消息的最大次数
    private static final int MAX_UN_REC_PONG_TIMES = 3;

    private volatile int lossConnectCount = 0;
    // 客户端连续N次没有收到服务端的pong消息  计数器
    private int unRecPongTimes = 0;

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        NettyClient client = this.getClient();
        client.defaultChannel(ctx.channel());
        super.channelRegistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        NettyClient client = getClient();
        client.defaultChannel(ctx.channel());

        long clientId = client.getClientId();
        log.info("[netty client id: {}] 激活成功", clientId);
        super.channelActive(ctx);
    }

    //采用管道空闲时心跳机制
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            NettyClient client = getClient();
            long clientId = client.getClientId();
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                if (lossConnectCount >= MAX_UN_REC_PONG_TIMES) {
                    // 3次心跳客户端都没有给心跳回复，则关闭连接
                    ctx.channel().close();
                    log.error("[netty client id: {}] heartbeat timeout, close.", clientId);
                }
            } else if (event.state() == IdleState.WRITER_IDLE) {
                //管道写入空闲的时候进行心跳
                log.info("[netty client id: {}] heartbeat ", clientId);
                sendHeartBeat(ctx);
            } else if (event.state() == IdleState.ALL_IDLE) {
            }
            //时间维度的检测
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    //重置丢失连接的次数
    private void resetLoss() {
        lossConnectCount = 0;
    }

    //发送心跳数据
    private void sendHeartBeat(ChannelHandlerContext ctx) {
        long clientId = getClient().getClientId();
        writeAndFlush(ctx.channel(), "this is client " + clientId, Cmd.HEARTBEAT);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("[netty client id: {}] exceptionCaught 客户端 error= {}", getClientId(), cause.getMessage(), cause);
        //NettyClient client = getClient();
        // client.close();
        super.exceptionCaught(ctx, cause);
    }

    /**
     * 处理断开重连
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("检测到心跳服务器断开！！！");
        NettyClient client = getClient();
        //关闭客户端
        //client.close();

        final EventLoop eventLoop = ctx.channel().eventLoop();
        eventLoop.schedule(() -> getClient().restart(), 10L, TimeUnit.SECONDS);
        //重启
        client.restart();
        super.channelInactive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SafeFrame<String> msg) {
        //无论收到服务端的任何信息，说明连接已经可用了，直接重置丢失次数
        resetLoss();
        //心跳信息处理
        if (Cmd.HEARTBEAT.code() == msg.getCmd()) {
            log.info("[netty client id: {}] client receive heartbeat req.", getClient().getClientId());
            sendHeartBeat(ctx);
        } else {
            //其他处理器处理
            ctx.fireChannelRead(msg);
        }
    }


}
