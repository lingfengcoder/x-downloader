package com.lingfeng.rpc.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
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
public class MyServerHandler extends ChannelInboundHandlerAdapter {

    private volatile int serverId;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //获取客户端发送过来的消息
        ByteBuf byteBuf = (ByteBuf) msg;
        log.info("[netty server handler serverId={}] 收到客户端{}", serverId, ctx.channel().remoteAddress() + "发送的消息：" + byteBuf.toString(CharsetUtil.UTF_8));
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        //发送消息给客户端
        ctx.writeAndFlush(Unpooled.copiedBuffer("服务端已收到消息，并给你发送一个问号?", CharsetUtil.UTF_8));
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                log.info("[netty server handler serverId={}]  => Reader Idle", serverId);
                ctx.writeAndFlush("[netty server handler] 读取等待：客户端你在吗[ctx.close()]{我结尾是一个换行符用于处理半包粘包}... ...\r\n");
                ctx.close();
            } else if (e.state() == IdleState.WRITER_IDLE) {
                log.info("[netty server handler serverId={}]   => Write Idle", serverId);
                ctx.writeAndFlush("[netty server handler] 写入等待： 客户端你在吗{我结尾是一个换行符用于处理半包粘包}... ...\r\n");
            } else if (e.state() == IdleState.ALL_IDLE) {
                log.info("[netty server handler serverId={}]  => All_IDLE", serverId);
                ctx.writeAndFlush("[netty server handler] 全部时间： 客户端你在吗{我结尾是一个换行符用于处理半包粘包}... ...\r\n");
            }
        }
        ctx.flush();
    }

    //当客户端主动断开服务端的链接后，这个通道就是不活跃的。也就是说客户端与服务端的关闭了通信通道并且不可以传输数据
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.error("[netty server handler serverId={}]  客户端断开链接 {}", serverId, ctx.channel().localAddress().toString());

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("[netty server handler serverId={}]  exceptionCaught = {} ", serverId, cause.getMessage(), cause);
        //发生异常，关闭通道
        ctx.close();
    }
}
