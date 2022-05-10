package com.lingfeng.rpc.client;

import com.lingfeng.rpc.model.Message;
import com.lingfeng.rpc.model.MessageType;
import com.lingfeng.rpc.trans.BizFrame;
import com.lingfeng.rpc.trans.MessageTrans;
import com.lingfeng.rpc.util.GsonTool;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.StringUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
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
        BizFrame bizFrame = MessageTrans.buildMsg("hi I m client " + clientId, clientId);
        ctx.writeAndFlush(bizFrame);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //接收服务端发送过来的消息
//        Message<Object> message = MessageTrans.parseStr((ByteBuf) msg);
        BizFrame frame = (BizFrame) msg;
        log.info("frame = {}", frame);
        Message<Object> message = MessageTrans.parseStr(frame);
        log.info("message = {}", message);
        if (message != null) {
            //对消息进行分发处理
            MessageDispatcher.dispatcher(client, message);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        log.error("[netty client id: {}] exceptionCaught 客户端 error= {}", clientId, cause.getMessage(), cause);
        client.close();
        client.restart();
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
        client.restart();
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
//        ByteBuf byteBuf = MessageTrans.buildMsg(msg, clientId);
        BizFrame frame = MessageTrans.buildMsg(msg, clientId);
        try {
            channel.writeAndFlush(frame);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            //byteBuf.release();
        }

    }


}
