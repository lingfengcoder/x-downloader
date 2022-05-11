package com.lingfeng.rpc.coder;

import io.netty.channel.*;

/**
 * @Author: wz
 * @Date: 2022/5/11 15:15
 * @Description:
 */
public interface Coder {
    ChannelHandlerAdapter encode();

    ChannelHandlerAdapter decode();

    ChannelHandlerAdapter type();
}
