package com.lingfeng.biz.downloader.netty;

import com.lingfeng.biz.downloader.NettyRpcClient;
import com.lingfeng.rpc.client.handler.AbsClientHandler;
import com.lingfeng.rpc.data.Frame;
import com.lingfeng.rpc.frame.SafeFrame;
import io.netty.channel.ChannelHandlerContext;

/**
 * @Author: wz
 * @Date: 2022/5/26 13:58
 * @Description:
 */
public class BasicHandler extends AbsClientHandler<SafeFrame<Frame<?>>> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SafeFrame<Frame<?>> msg) throws Exception {

    }


    public void register() {
        NettyRpcClient rpcClient = NettyRpcClient.getInstance();
        String clientId = rpcClient.getClientId();
    }
}
