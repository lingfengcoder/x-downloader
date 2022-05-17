package com.lingfeng.rpc.demo;

import com.lingfeng.rpc.client.handler.AbsClientHandler;
import com.lingfeng.rpc.frame.SafeFrame;
import com.lingfeng.rpc.server.handler.AbsServerHandler;
import com.lingfeng.rpc.server.nettyserver.AbsNettyServer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
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
//@ChannelHandler.Sharable
public class DemoServerHandler extends AbsServerHandler {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object frame) throws Exception {
        try {
//            String data = frame.toString();
//            log.info(" Server get msg ={}", data);
//            SafeFrame msg = (SafeFrame) frame;

            String data = parseStr((ByteBuf) frame);
            log.info(" client get msg ={}", data);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //ctx.fireChannelRead(frame);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        ChannelId id = channel.id();
        getServer().addChannel(id.asLongText(), channel);
        super.channelActive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error(cause.getMessage(), cause);
        super.exceptionCaught(ctx, cause);
    }
}
