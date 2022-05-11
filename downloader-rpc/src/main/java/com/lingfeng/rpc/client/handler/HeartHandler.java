package com.lingfeng.rpc.client.handler;


import com.lingfeng.rpc.client.nettyclient.NettyClient;
import com.lingfeng.rpc.constant.Cmd;
import com.lingfeng.rpc.frame.SafeFrame;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.concurrent.TimeUnit;


@Slf4j
public class HeartHandler extends BaseClientHandler<SafeFrame<String>> {

    public final static String NAME = "idleTimeoutHandler";
    private final DateFormat sf = new SimpleDateFormat("HH:mm:ss");

    private NettyClient nettyClient;
    private int heartbeatCount = 0;
    private final static String CLIENTID = get("spring.netty.clientId");
    private long ccTime = 0;//缓存发送时间 单位毫秒

    // 定义客户端没有收到服务端的pong消息的最大次数
    private static final int MAX_UN_REC_PONG_TIMES = 3;

    // 多长时间未请求后，发送心跳
    private static final int WRITE_WAIT_SECONDS = 5;//暂时未使用

    // 隔N秒后重连
    private static final int RE_CONN_WAIT_SECONDS = 5;//暂时未使用

    // 客户端连续N次没有收到服务端的pong消息  计数器
    private int unRecPongTimes = 0;

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            String type = "";
            if (event.state() == IdleState.READER_IDLE) {
                type = "read idle";
            } else if (event.state() == IdleState.WRITER_IDLE) {
                type = "write idle";
            } else if (event.state() == IdleState.ALL_IDLE) {
                type = "all idle";
            }
            if (unRecPongTimes < MAX_UN_REC_PONG_TIMES) {
                sendPingMsg(ctx, CLIENTID);
                unRecPongTimes++;
            } else {
                ctx.channel().close();
            }

        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    /**
     * 发送ping消息
     *
     * @param context
     */
    protected void sendPingMsg(ChannelHandlerContext context, String client) {
        heartbeatCount++;
        ccTime = System.currentTimeMillis();
//        context.writeAndFlush(setCmd(Command.CommandType.PING))
        //	log.info("Client sent ping msg to " + context.channel().remoteAddress() + ", count: " + heartbeatCount);
    }

    /**
     * @return返回微秒 如果有更深一步的业务可使用
     */
    public static Long getmicTime() {
        Long cutime = System.currentTimeMillis() * 1000; // 微秒
        Long nanoTime = System.nanoTime(); // 纳秒
        return cutime + (nanoTime - nanoTime / 1000000 * 1000000) / 1000;
    }

    /**
     * 处理断开重连
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("检测到心跳服务器断开！！！");
        final EventLoop eventLoop = ctx.channel().eventLoop();
        eventLoop.schedule(() -> getClient().restart(new Bootstrap(), eventLoop), 10L, TimeUnit.SECONDS);
        super.channelInactive(ctx);
    }


    private volatile Thread writeThread = null;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SafeFrame<String> msg) {
        if (Cmd.HEARTBEAT.code() == msg.getCmd()) {
            log.info("client receive heartbeat req.");
            long clientId = getClient().getClientId();
            writeAndFlush("dance", Cmd.HEARTBEAT);
        } else {
            ctx.fireChannelRead(msg);
        }
        // if (msg.getCmd().equals(Command.CommandType.PONG)) {

//            //接收到server发送的pong指令
//            unRecPongTimes = 0;
//            //计算ping值
//            long ping = (System.currentTimeMillis() - ccTime) / 2;
//            log.info("客户端和服务器的ping是" + ping + "ms");
//            //log.info(msg.getData());
//            StringRedisTemplate redisTemplate = SpringBeanFactory.getBean(StringRedisTemplate.class);
//            MonitorVo monitorVo = new MonitorVo();
//            monitorVo.setClientId(CLIENTID);
//            monitorVo.setDateList(sf.format(new Date()));
//            monitorVo.setValueList(ping);
//            redisTemplate.opsForList().leftPush("heart:monitor:" + CLIENTID, JSON.toJSONString(monitorVo));
//            //计算此时的时间
//            //	log.info(msg.getData());

//    }

    }

    public static String get(String key) {
        Resource resource = new ClassPathResource("application.properties");
        Properties props = null;
        String property = "";
        try {
            props = PropertiesLoaderUtils.loadProperties(resource);
            property = props.getProperty(key);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return property;

    }

    public static void main(String[] args) throws IOException {
        String s = get("spring.netty.clientId");
        System.out.println(s);
    }

}
