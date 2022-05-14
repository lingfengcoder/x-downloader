package com.lingfeng.rpc.coder.biz;

import com.lingfeng.rpc.frame.SafeFrame;
import com.lingfeng.rpc.frame.SimpleFrame;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.charset.StandardCharsets;

/**
 * @Author: wz
 * @Date: 2022/5/10 14:54
 * @Description: 简单消息编码器
 */
public class SimpleEncoder extends MessageToByteEncoder<SimpleFrame> {

    //simpleFrame
    // private byte type;
    //    private long client;
    //    private int length;
    //    private String content;
    @Override
    protected void encode(ChannelHandlerContext ctx, SimpleFrame safeFrame, ByteBuf out) throws Exception {
//        byte[] bytes = safeFrame.getContent().getBytes(StandardCharsets.UTF_8);
        byte[] bytes = null;//safeFrame.getContent().getBytes(StandardCharsets.UTF_8);
        safeFrame.setLength(bytes.length);

        out.writeByte(safeFrame.getType());//1
        out.writeLong(safeFrame.getClient());//8
        out.writeInt(bytes.length);//4
        out.writeBytes(bytes);//len
    }
}
