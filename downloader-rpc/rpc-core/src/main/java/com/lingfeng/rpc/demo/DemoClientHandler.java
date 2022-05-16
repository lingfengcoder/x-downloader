package com.lingfeng.rpc.demo;

import com.lingfeng.rpc.client.handler.AbsClientHandler;
import com.lingfeng.rpc.client.nettyclient.NettyClient;
import com.lingfeng.rpc.coder.safe.DataFrame;
import com.lingfeng.rpc.constant.Cmd;
import com.lingfeng.rpc.frame.SafeFrame;
import com.lingfeng.rpc.util.SystemClock;
import com.lingfeng.rpc.util.TimeUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
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
public class DemoClientHandler extends AbsClientHandler {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object frame) throws Exception {
        String data = parseStr((ByteBuf) frame);
        log.info(" client get msg ={}", data);
        ctx.fireChannelRead(frame);
    }


}
