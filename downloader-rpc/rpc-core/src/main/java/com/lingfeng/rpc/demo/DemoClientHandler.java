package com.lingfeng.rpc.demo;

import cn.hutool.core.util.RandomUtil;
import com.lingfeng.rpc.client.handler.AbsClientHandler;
import com.lingfeng.rpc.client.nettyclient.NettyClient;
import com.lingfeng.rpc.coder.safe.DataFrame;
import com.lingfeng.rpc.constant.Cmd;
import com.lingfeng.rpc.frame.SafeFrame;
import com.lingfeng.rpc.trans.MessageTrans;
import com.lingfeng.rpc.util.GsonTool;
import com.lingfeng.rpc.util.SystemClock;
import com.lingfeng.rpc.util.TimeUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

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
        try {
            String msg = parseStr((ByteBuf) frame);
//            SafeFrame msg = (SafeFrame) frame;
            log.info(" client get msg ={}", msg);
            Channel channel = ctx.channel();
            String data = "client 已经获取到消息 " + TimeUtil.formatDate(new Date());

            ByteBuf byteBuf = buildMsg(GsonTool.toJson(data));
            channel.writeAndFlush(byteBuf);
//            SafeFrame<String> retData = MessageTrans.dataFrame(data, Cmd.REQUEST, getClientId());
//            channel.writeAndFlush(retData);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // ctx.fireChannelRead(frame);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error(cause.getMessage(), cause);
        super.exceptionCaught(ctx, cause);
    }
}
