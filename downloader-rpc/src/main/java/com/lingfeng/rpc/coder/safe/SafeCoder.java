package com.lingfeng.rpc.coder.safe;

import com.lingfeng.rpc.coder.Coder;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * @Author: wz
 * @Date: 2022/5/11 15:11
 * @Description:
 */
public class SafeCoder implements Coder {
    public final static int MAXFRAMELENGTH = Integer.MAX_VALUE;
    public final static int LENGTHFIELDOFFSET = 51;
    public final static int LENGTHFIELDLENGTH = 4;
    public final static int LENGTHADJUSTMENT = 0;
    public final static int INITIALBYTESTOSTRIP = 0;

    @Override
    public ChannelOutboundHandlerAdapter encode() {
        return new SafeEncoder();
    }

    @Override
    public ChannelInboundHandlerAdapter decode() {
        return new SafeDecoder();
    }

    @Override
    public ChannelInboundHandlerAdapter type() {
        return new SafeLengthFieldDecoder(MAXFRAMELENGTH, LENGTHFIELDOFFSET, LENGTHFIELDLENGTH, LENGTHADJUSTMENT, INITIALBYTESTOSTRIP);
    }


}
