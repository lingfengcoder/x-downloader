package com.lingfeng.rpc.trans;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.charset.StandardCharsets;

/**
 * @Author: wz
 * @Date: 2022/5/10 14:54
 * @Description: 自定义消息编码器
 */
public class BizEncoder extends MessageToByteEncoder<BizFrame> {
    @Override
    protected void encode(ChannelHandlerContext ctx, BizFrame bizFrame, ByteBuf out) throws Exception {
        out.writeByte(bizFrame.getType());
        out.writeInt(bizFrame.getLength());
        out.writeBytes(bizFrame.getContent().getBytes(StandardCharsets.UTF_8));
    }
}