package com.lingfeng.rpc.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * @Author: wz
 * @Date: 2022/5/7 18:29
 * @Description:
 */
@Slf4j
@Setter
@Getter
@Accessors(chain = true)
@ChannelHandler.Sharable
public class MyClientHandler extends ChannelInboundHandlerAdapter {


    private volatile int clientId;
    private volatile ChannelHandlerContext channel;
    private volatile NettyClient client;

    private volatile SendBuffer<String> sender;

    public MyClientHandler() {
        sender = new SendBuffer<>(10);
    }

    public MyClientHandler setSender(SendBuffer<String> sender) {
        this.sender = sender;
        return this;
    }

    public MyClientHandler setClient(NettyClient client) {
        this.client = client;
        return this;
    }

    public void setChannel(ChannelHandlerContext channel) {
        this.channel = channel;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        this.setChannel(ctx);
        log.info("[netty client id: {}] 客户端注册", clientId);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //发送消息到服务端
        ctx.writeAndFlush(buildMsg("hi I m client " + clientId));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //接收服务端发送过来的消息
        log.info("[netty client id: {}] 收到服务端{}的消息：{}", clientId, ctx.channel().remoteAddress(), parseStr((ByteBuf) msg));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        log.error("[netty client id: {}] exceptionCaught 客户端 error= {}", clientId, cause.getMessage(), cause);
        client.close();
    }


    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        log.info("[netty client id: {}]=== channelUnregistered ===", clientId);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        log.info("[netty client id: {}] === channelInactive ===", clientId);
        client.close();
        do {
            TimeUnit.SECONDS.sleep(2);
            client.start();
        } while (client.state() != State.RUNNING.getCode());
    }

    public void write(String msg) {
        while (channel == null) {
            // log.info("");
        }
        int state = State.RUNNING.getCode();
        if (client.state() != state) {
            throw new RuntimeException("[netty client id: " + this.getClientId() + "] client state error, state=" + state);
        }

        //  channel.writeAndFlush(msg);
        channel.writeAndFlush(buildMsg(msg));

    }

    private ByteBuf buildMsg(String msg) {
        return Unpooled.copiedBuffer(msg, CharsetUtil.UTF_8);
    }

    private String parseStr(ByteBuf byteBuf) {
        return byteBuf.toString(CharsetUtil.UTF_8);
    }

}
