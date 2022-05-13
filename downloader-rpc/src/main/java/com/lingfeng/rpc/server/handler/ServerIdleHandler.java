package com.lingfeng.rpc.server.handler;

import io.netty.channel.ChannelHandler;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * @Author: wz
 * @Date: 2022/5/11 19:25
 * @Description:
 */
//@ChannelHandler.Sharable
public class ServerIdleHandler extends IdleStateHandler {

    public final static String NAME = "idleStateHandler";
    private final static int READER_IDLE_TIME_SECONDS = 0;//读操作空闲20秒
    private final static int WRITER_IDLE_TIME_SECONDS = 5;//写操作空闲20秒
    private final static int ALL_IDLE_TIME_SECONDS = 0;//读写全部空闲40秒

    public ServerIdleHandler(long readerIdleTime, long writerIdleTime, long allIdleTime, TimeUnit unit) {
        super(readerIdleTime, writerIdleTime, allIdleTime, unit);
    }


    //空闲处理器
    public static ServerIdleHandler getIdleHandler() {
        return new ServerIdleHandler(READER_IDLE_TIME_SECONDS
                , WRITER_IDLE_TIME_SECONDS, ALL_IDLE_TIME_SECONDS, TimeUnit.SECONDS);
    }
}
