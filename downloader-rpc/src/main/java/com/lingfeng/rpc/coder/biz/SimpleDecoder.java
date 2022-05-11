package com.lingfeng.rpc.coder.biz;

import com.lingfeng.rpc.frame.SafeFrame;
import com.lingfeng.rpc.frame.SimpleFrame;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;


import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @Author: wz
 * @Date: 2022/5/10 14:53
 * @Description: see #{simpleFrame}
 */
@Slf4j
public class SimpleDecoder extends ByteToMessageDecoder {


    /**
     * maxFrameLength      帧的最大长度
     * lengthFieldOffset   length字段偏移的地址
     * lengthFieldLength   length字段所占的字节长
     * lengthAdjustment    修改帧数据长度字段中定义的值，可以为负数 因为有时候我们习惯把头部记入长度,若为负数,则说明要推后多少个字段
     * initialBytesToStrip 解析时候跳过多少个长度
     * failFast            为true，当frame长度超过maxFrameLength时立即报TooLongFrameException异常，
     * 为false，读取完整个帧再报异
     * // super(9999, 1, 4, 0, 0);
     */
    //simpleFrame
    // private byte type;
    //    private long client;
    //    private int length;
    //    private String content;
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        try {
            //在这里调用父类的方法
            if (in == null) {
                return;
            }
            //读取type字段
            byte type = in.readByte();//1
            //clientId
            long client = in.readLong();//8
            //读取length字段
            int length = in.readInt();//4
            if (in.readableBytes() != length) {
                throw new RuntimeException("长度与标记不符");
            }
            //读取body
            byte[] bytes = new byte[in.readableBytes()];
            in.readBytes(bytes);
            SimpleFrame safeFrame = SimpleFrame.builder()
                    .client(client)
                    .length(length)
                    .type(type)
                    .content(new String(bytes, StandardCharsets.UTF_8)).build();
            out.add(safeFrame);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
