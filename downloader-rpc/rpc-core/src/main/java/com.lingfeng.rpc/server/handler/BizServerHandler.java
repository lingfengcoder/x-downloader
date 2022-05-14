package com.lingfeng.rpc.server.handler;

import com.lingfeng.rpc.coder.safe.DataFrame;
import com.lingfeng.rpc.constant.Cmd;
import com.lingfeng.rpc.frame.SafeFrame;
import com.lingfeng.rpc.model.Address;
import com.lingfeng.rpc.server.dispatcher.MessageDispatcher;
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
//@ChannelHandler.Sharable
public class BizServerHandler extends AbsServerHandler<SafeFrame<Address>> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SafeFrame safeFrame) throws Exception {
        log.info("MyServerHandler frame = {}", safeFrame);
//        Message<Object> message = MessageTrans.parseStr((ByteBuf) msg);
        if (safeFrame != null) {
            long clientId = safeFrame.getClient();
            Channel channel = ctx.channel();
            MessageDispatcher.dispatcher(clientId, safeFrame.getContent());
            DataFrame<String> frame = new DataFrame<>();
            frame.setData("服务端已收到消息");
            writeAndFlush(channel, frame, Cmd.REQUEST);
        } else {
            ctx.fireChannelRead(safeFrame);
        }
        //channelRead0 不再需要 ReferenceCountUtil.release(msg);
    }
}
