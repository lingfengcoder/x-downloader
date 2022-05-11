package com.lingfeng.rpc.client.handler;

import com.lingfeng.rpc.constant.Cmd;
import com.lingfeng.rpc.frame.SafeFrame;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;


/**
 * @author wz
 * @Description 心跳处理器
 * @date 2022/5/11 13:53
 */

@Slf4j
@ChannelHandler.Sharable
public class HeartbeatClientHandler extends BaseClientHandler<SafeFrame<String>> {
    private int lossConnectCount = 0;

    //空闲时的心跳
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                lossConnectCount++;
                if (lossConnectCount >= 5) {
                    // 5次服务端未返回心跳应答消息，则关闭连接
                    ctx.channel().close();
                    log.error("heartbeat timeout, close.");
                    return;
                }
            } else if (event.state() == IdleState.WRITER_IDLE) {
                log.warn("heartbeat timeout. lossCount: {}", lossConnectCount);
                // sent heartbeat msg
                //ctx.writeAndFlush(buildHtReqMsg());
            }
        } else {
            ctx.fireUserEventTriggered(evt);
        }
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SafeFrame<String> msg) throws Exception {
        lossConnectCount = 0;
        //如果是心跳包
        if (Cmd.HEARTBEAT.code() == msg.getCmd()) {
            log.info("client receive heartbeat req.");
            long clientId = getClient().getClientId();
            writeAndFlush("dance", Cmd.HEARTBEAT);
        } else {
            ctx.fireChannelRead(msg);
        }
    }

}
