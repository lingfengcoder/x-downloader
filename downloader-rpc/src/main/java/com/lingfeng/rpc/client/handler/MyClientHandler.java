package com.lingfeng.rpc.client.handler;

import com.lingfeng.rpc.client.nettyclient.BaseNettyClient;
import com.lingfeng.rpc.client.MessageDispatcher;
import com.lingfeng.rpc.coder.safe.SubReqFrame;
import com.lingfeng.rpc.constant.Cmd;
import com.lingfeng.rpc.frame.SafeFrame;
import io.netty.channel.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

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
public class MyClientHandler extends BaseClientHandler<SafeFrame<SubReqFrame<?>>> {

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        this.setChannel(ctx);
        log.info("[netty client id: {}] 客户端注册", getClientId());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SafeFrame<SubReqFrame<?>> frame) throws Exception {
        this.setChannel(ctx);
        int hashCode = ctx.hashCode();
        //接收服务端发送过来的消息
//        Message<Object> message = MessageTrans.parseStr((ByteBuf) msg);
        log.info("ctx hashCode={} client get frame = {}", hashCode, frame);
        if (frame != null) {
            if (Cmd.REQUEST.code() == frame.getCmd()) {
                SubReqFrame<?> content = frame.getContent();
                log.info("client TempData = {}", content);
                //对消息进行分发处理
                MessageDispatcher.dispatcher(getClient(), frame.getContent());
            }
        }
        // ReferenceCountUtil.release(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        log.error("[netty client id: {}] exceptionCaught 客户端 error= {}", getClientId(), cause.getMessage(), cause);
        BaseNettyClient client = getClient();
        client.close();
        client.restart();
    }


    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        log.info("[netty client id: {}]=== channelUnregistered ===", getClientId());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        log.info("[netty client id: {}] === channelInactive ===", getClientId());
        BaseNettyClient client = getClient();
        client.close();
        client.restart();
    }

}
