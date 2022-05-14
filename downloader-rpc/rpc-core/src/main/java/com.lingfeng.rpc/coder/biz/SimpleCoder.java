package com.lingfeng.rpc.coder.biz;

import com.lingfeng.rpc.coder.Coder;
import io.netty.channel.*;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * @Author: wz
 * @Date: 2022/5/11 15:11
 * @Description:
 */
public class SimpleCoder implements Coder {
    public final static int MAXFRAMELENGTH = Integer.MAX_VALUE;
    public final static int LENGTHFIELDOFFSET = 9;//前面9位都是头部数据 从第十位开始是length
    public final static int LENGTHFIELDLENGTH = 4;//length 数据帧占用的字段长度
    public final static int LENGTHADJUSTMENT = 0;
    public final static int INITIALBYTESTOSTRIP = 0;

    @Override
    public ChannelHandlerAdapter encode() {
        return new SimpleEncoder();
    }

    @Override
    public ChannelHandlerAdapter decode() {
        return new SimpleDecoder();
    }

    @Override
    public ChannelHandlerAdapter type() {
        return new LengthFieldBasedFrameDecoder(MAXFRAMELENGTH, LENGTHFIELDOFFSET, LENGTHFIELDLENGTH, LENGTHADJUSTMENT, INITIALBYTESTOSTRIP);
    }
}
