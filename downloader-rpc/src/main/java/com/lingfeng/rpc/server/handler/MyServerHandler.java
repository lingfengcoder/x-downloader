package com.lingfeng.rpc.server.handler;

import com.lingfeng.rpc.constant.Cmd;
import com.lingfeng.rpc.frame.SafeFrame;
import com.lingfeng.rpc.model.Address;
import com.lingfeng.rpc.server.MessageDispatcher;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;


/**
 * @Author: wz
 * @Date: 2022/5/7 18:33
 * @Description:
 */
@Slf4j
@Setter
@Accessors(chain = true)
@ChannelHandler.Sharable
public class MyServerHandler extends BaseServerHandler<SafeFrame<Address>> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SafeFrame safeFrame) throws Exception {
        log.info("MyServerHandler frame = {}", safeFrame);
//        Message<Object> message = MessageTrans.parseStr((ByteBuf) msg);
        if (safeFrame != null) {
            long clientId = safeFrame.getClient();
            Channel channel = ctx.channel();
            addChannel(clientId, channel);
            MessageDispatcher.dispatcher(clientId, safeFrame.getContent());
            writeAndFlush("服务端已收到消息", Cmd.REQUEST);
        } else {
            ctx.fireChannelRead(safeFrame);
        }
        //channelRead0 不再需要 ReferenceCountUtil.release(msg);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                log.info("[netty server handler serverId={}]  => Reader Idle", getServerId());
                ctx.writeAndFlush("[netty server handler] 读取等待：客户端你在吗[ctx.close()]{我结尾是一个换行符用于处理半包粘包}... ...\r\n");
                ctx.close();
            } else if (e.state() == IdleState.WRITER_IDLE) {
                log.info("[netty server handler serverId={}]   => Write Idle", getServerId());
                ctx.writeAndFlush("[netty server handler] 写入等待： 客户端你在吗{我结尾是一个换行符用于处理半包粘包}... ...\r\n");
            } else if (e.state() == IdleState.ALL_IDLE) {
                log.info("[netty server handler serverId={}]  => All_IDLE", getServerId());
                ctx.writeAndFlush("[netty server handler] 全部时间： 客户端你在吗{我结尾是一个换行符用于处理半包粘包}... ...\r\n");
            }
        }
        ctx.flush();
    }

}
