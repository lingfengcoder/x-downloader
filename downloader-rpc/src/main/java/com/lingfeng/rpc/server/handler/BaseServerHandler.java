package com.lingfeng.rpc.server.handler;

import com.lingfeng.rpc.constant.Cmd;
import com.lingfeng.rpc.constant.State;
import com.lingfeng.rpc.frame.SafeFrame;
import com.lingfeng.rpc.server.NettyServer;
import com.lingfeng.rpc.trans.MessageTrans;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @Author: wz
 * @Date: 2022/5/11 14:16
 * @Description:
 */
@Slf4j
public abstract class BaseServerHandler<T> extends SimpleChannelInboundHandler<T> {
    private volatile NettyServer server;
    private volatile ChannelHandlerContext channel;
    //连接通道的集合
    private final ConcurrentHashMap<Long, Channel> channels = new ConcurrentHashMap<>();

    public void closeChannel(Long channelId) {
        Channel channel = channels.get(channelId);
        if (channel != null) {
            log.info("关闭channel{}", channelId);
            channel.close();
            channels.remove(channelId);
        }
    }

    public void addChannel(Long clientId, Channel channel) {
        // InetSocketAddress socketAddress = (InetSocketAddress) channel.remoteAddress();
        if (!channels.containsKey(clientId)) {
            channels.put(clientId, channel);
        }
    }

    public long getServerId() {
        return server.getServerId();
    }

    @Override
    //管道被激活的时候
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.setChannel(ctx);
        super.channelActive(ctx);
    }


    //当客户端主动断开服务端的链接后，这个通道就是不活跃的。也就是说客户端与服务端的关闭了通信通道并且不可以传输数据
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        log.error("[netty server handler serverId={}]  客户端断开链接 {}", getServerId(), ctx.channel().localAddress().toString());

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        log.error("[netty server handler serverId={}]  exceptionCaught = {} ", getServerId(), cause.getMessage(), cause);
        //发生异常，关闭通道
        ctx.close();
    }

    public void writeAndFlush(String msg, Cmd type, ChannelHandlerContext channel) {
        this.setChannel(channel);
        //如果channel没有注册好 则循环等待
        accessChannel();
        accessClientState();
        log.info("ctx hashCode={} [write]", channel.hashCode());
        SafeFrame<String> safeFrame = MessageTrans.heartbeatFrame(getServerId());
        channel.writeAndFlush(safeFrame);
    }

    public <T extends Serializable> void writeAndFlush(T msg, Cmd type) {
        //如果channel没有注册好 则循环等待
        accessChannel();
        accessClientState();
        log.info("ctx hashCode={} [write]", channel.hashCode());
        SafeFrame<T> safeFrame = null;
        switch (type) {
            case HEARTBEAT:
                channel.writeAndFlush(MessageTrans.heartbeatFrame(getServerId()));
                return;
            case REQUEST:
                channel.writeAndFlush(MessageTrans.dataFrame(msg, getServerId()));
                break;
            case RESPONSE:
//                channel.writeAndFlush();
                break;
        }
    }

    //判断client的状态，如果已经关闭
    private void accessClientState() {
        int state = State.RUNNING.getCode();
        boolean removed = channel.isRemoved();
        if (server.state() != state || removed) {
            throw new RuntimeException("[netty client id: " + getServerId() + "] client state error, state=" + state);
        }
    }

    //如果channel没有注册好 则循环等待
    private void accessChannel() {
        while (channel == null) {
            try {
                TimeUnit.MILLISECONDS.sleep(200);
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public NettyServer getServer() {
        return server;
    }

    public BaseServerHandler<T> setServer(NettyServer server) {
        this.server = server;
        return this;
    }

    public ChannelHandlerContext getChannel() {
        return channel;
    }

    public BaseServerHandler<T> setChannel(ChannelHandlerContext channel) {
        this.channel = channel;
        return this;
    }

    public ConcurrentHashMap<Long, Channel> getChannels() {
        return channels;
    }
}
