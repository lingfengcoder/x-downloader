package com.lingfeng.rpc.coder.safe;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * @Author: wz
 * @Date: 2022/5/12 16:58
 * @Description:
 */
public class SafeLengthFieldDecoder extends LengthFieldBasedFrameDecoder {

    public SafeLengthFieldDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }
}
