package com.lingfeng.biz.downloader.netty;

import com.lingfeng.biz.downloader.NettyRpcClient;
import com.lingfeng.biz.downloader.model.BasicCmd;
import com.lingfeng.biz.downloader.model.BasicFrame;
import com.lingfeng.biz.downloader.model.TaskCmd;
import com.lingfeng.biz.downloader.model.TaskFrame;
import com.lingfeng.rpc.client.handler.AbsClientHandler;
import com.lingfeng.rpc.client.nettyclient.BizNettyClient;
import com.lingfeng.rpc.constant.Cmd;
import com.lingfeng.rpc.data.Frame;
import com.lingfeng.rpc.frame.SafeFrame;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
        }else {
            ctx.fireChannelRead(data);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        register();
    }

    //注册客户端到服务端
    public void register() {
        NettyRpcClient client = NettyRpcClient.getInstance();
        BizNettyClient nettyClient = client.getNettyClient();
        String clientId = client.getClientId();
        //注册帧
        BasicFrame<Object> frame = BasicFrame.builder()
                .cmd(BasicCmd.REG)
                .clientId(clientId)
                .build();
        nettyClient.writeAndFlush(frame, Cmd.REQUEST);
    }
}
