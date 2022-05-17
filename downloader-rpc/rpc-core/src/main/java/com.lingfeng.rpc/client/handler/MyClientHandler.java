package com.lingfeng.rpc.client.handler;

import com.lingfeng.rpc.client.MessageDispatcher;
import com.lingfeng.rpc.client.nettyclient.NettyClient;
import com.lingfeng.rpc.coder.safe.DataFrame;
import com.lingfeng.rpc.constant.Cmd;
import com.lingfeng.rpc.frame.SafeFrame;
import com.lingfeng.rpc.util.SystemClock;
import com.lingfeng.rpc.util.TimeUtil;
import io.netty.channel.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: wz
 * @Date: 2022/5/7 18:29
 * @Description:
 */
@Slf4j
@Setter
@Getter
@Accessors(chain = true)
//@ChannelHandler.Sharable
public class MyClientHandler extends AbsClientHandler<SafeFrame<DataFrame<?>>> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SafeFrame<DataFrame<?>> frame) throws Exception {
        NettyClient client = getClient();
//        client.defaultChannel(ctx.channel());
        int hashCode = ctx.hashCode();
        //接收服务端发送过来的消息
//        Message<Object> message = MessageTrans.parseStr((ByteBuf) msg);
        log.info("ctx hashCode={} client get frame = {}", hashCode, frame);
        if (frame != null) {
            if (Cmd.REQUEST.code() == frame.getCmd()) {
                DataFrame<?> content = frame.getContent();
                log.info("client TempData = {}", content);
                DataFrame<String> resp = new DataFrame<>();
                resp.setData("客户端已经处理了消息:" + TimeUtil.formatDate(SystemClock.now()));
                //对消息进行分发处理
                // MessageDispatcher.dispatcher(ctx.channel(), getClient(), frame.getContent());
                writeAndFlush(ctx, resp, Cmd.RESPONSE);
            }
        }
        //ctx.fireChannelRead(frame);
    }


}
