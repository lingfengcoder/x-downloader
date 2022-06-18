package com.lingfeng.biz.downloader.netty;

import com.lingfeng.biz.downloader.NettyRpcClient;
import com.lingfeng.biz.downloader.model.BasicCmd;
import com.lingfeng.biz.downloader.model.BasicFrame;
import com.lingfeng.biz.downloader.netty.serverapi.RegisterAction;
import com.lingfeng.rpc.client.handler.AbsClientHandler;
import com.lingfeng.rpc.client.nettyclient.BizNettyClient;
import com.lingfeng.rpc.client.nettyclient.NettyClient;
import com.lingfeng.rpc.constant.Cmd;
import com.lingfeng.rpc.frame.SafeFrame;
import com.lingfeng.rpc.invoke.ProxySender;
import com.lingfeng.rpc.invoke.RemoteInvoke;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: wz
 * @Date: 2022/5/26 13:58
 * @Description:
 */
@Slf4j
public class BasicHandler extends AbsClientHandler<SafeFrame<BasicFrame<?>>> {


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SafeFrame<BasicFrame<?>> data) throws Exception {
        //处理服务器的返回帧
        if (data.getCmd() == Cmd.RESPONSE.code()) {
            BasicFrame<?> content = data.getContent();
            if (content.getCmd().code() == BasicCmd.REG.code()) {
                Object msg = content.getData();
                if (!"OK".equals(msg)) {
                    log.error("客户端注册失败！原因:{}", msg);
                    //todo 考虑重试注册
                }
            }
        } else {
            ctx.fireChannelRead(data);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        // register();
    }

    //注册客户端到服务端
    public void register() {
        log.info("do register========================================");
        NettyRpcClient client = NettyRpcClient.getInstance();
        BizNettyClient nettyClient = client.getNettyClient();
        Channel channel = nettyClient.getChannel();
        String clientId = client.getClientId();
        //注册帧
        BasicFrame<Object> frame = BasicFrame.builder()
                .cmd(BasicCmd.REG)
                .clientId(clientId)
                .build();
        RemoteInvoke instance = RemoteInvoke.getInstance(nettyClient, channel);
        //像调用本地方法一样，调用远程服务方法
        RegisterAction proxy = instance.getDynamicProxy(RegisterAction.class);
        int x = 2;
        for (int i = 0; i < x; i++) {
            proxy.register(frame);
        }
        log.info(" client invoke register RegisterAction");
    }
}
