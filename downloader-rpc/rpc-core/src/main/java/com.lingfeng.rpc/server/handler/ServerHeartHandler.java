package com.lingfeng.rpc.server.handler;


import com.lingfeng.rpc.constant.Cmd;
import com.lingfeng.rpc.constant.State;
import com.lingfeng.rpc.frame.SafeFrame;
import com.lingfeng.rpc.server.nettyserver.NettyServer;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;


import java.util.concurrent.TimeUnit;


@Slf4j
//@ChannelHandler.Sharable
public class ServerHeartHandler extends AbsServerHandler<SafeFrame<String>> {
    public final static String NAME = "idleTimeoutHandler";

    // 定义客户端没有收到服务端的pong消息的最大次数
    private static final int MAX_UN_REC_PONG_TIMES = 3;
    //最大重启次数
    private static final long MAX_RESTART_COUNT = 60; //60*1000 1分钟内尝试重启60次
    private volatile int lossConnectCount = 0;
    // 客户端连续N次没有收到服务端的pong消息  计数器
    private int unRecPongTimes = 0;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        NettyServer server = getServer();
        long serverId = server.getServerId();
        ChannelId id = ctx.channel().id();
        //加入channel
        server.addChannel(id.asLongText(), ctx.channel());
        resetLoss();

        log.info("[netty server id: {}] 激活成功", serverId);
        super.channelActive(ctx);
    }

    //采用管道空闲时心跳机制
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            NettyServer server = getServer();
            long serverId = server.getServerId();
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                if (lossConnectCount >= MAX_UN_REC_PONG_TIMES) {
                    // 3次心跳客户端都没有给心跳回复，则关闭连接
                    ctx.channel().close();
                    log.error("[netty serverId id: {}] heartbeat timeout, close.", serverId);
                }
            } else if (event.state() == IdleState.WRITER_IDLE) {
                //管道写入空闲的时候进行心跳
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
        writeAndFlush(ctx.channel(), "this is server heartbeat " + getServerId(), Cmd.HEARTBEAT);
    }

    /**
     * 处理断开重连
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("检测到心跳服务器断开！！！");
        //final EventLoop eventLoop = ctx.channel().eventLoop();
        // eventLoop.schedule(() -> getServer().restart(), 10L, TimeUnit.SECONDS);
        // loopRestart();
        NettyServer server = getServer();
        ctx.alloc();
        //关闭channel
        server.closeChannel(ctx.channel().id().asLongText());

        super.channelInactive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SafeFrame<String> msg) {
        //心跳信息处理
        if (Cmd.HEARTBEAT.code() == msg.getCmd()) {
            resetLoss();
            log.info("[netty server id: {}] server receive heartbeat req.", getServerId());
            //sendHeartBeat(ctx);
        } else {
            //其他处理器处理
            ctx.fireChannelRead(msg);
        }
    }


}
