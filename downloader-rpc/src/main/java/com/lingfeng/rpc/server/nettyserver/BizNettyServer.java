package com.lingfeng.rpc.server.nettyserver;


import com.lingfeng.rpc.server.listener.ServerReconnectFutureListener;
import com.lingfeng.rpc.server.handler.AbsServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;

import static java.lang.Thread.State.TERMINATED;

/**
 * @Author: wz
 * @Date: 2022/5/7 18:26
 * @Description:
 */
@Slf4j
public class BizNettyServer extends AbsNettyServer {


    private final static String PREFIX = "Biz-NettyServer";
    //服务线程
    private volatile Thread mainThread;

    private synchronized void start0() {
        log.info("[netty server id:{}] start0", serverId);
        if (mainThread != null) {
            if (state != 0) {
                log.error("[netty server id:{}]！{}", serverId, address);
                return;
            } else if (state == 0 && !mainThread.getState().equals(TERMINATED)) {
                log.error("[netty server id:{}] server mainThread 状态非停止！{}", serverId, mainThread.getState());
                return;
            }
        }
        state = 2;
        mainThread = new Thread(() -> {
            //创建bootstrap对象，配置参数
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            EventLoopGroup bossGroup = new NioEventLoopGroup();
            EventLoopGroup eventExecutors = new NioEventLoopGroup();
            connect(serverBootstrap, bossGroup, eventExecutors);
        });
        if (mainThread != null) {
            mainThread.setPriority(Thread.MAX_PRIORITY);
            mainThread.setDaemon(true);
            mainThread.setName(PREFIX + ":" + mainThread.getId());
            mainThread.start();
        }
    }

    private void connect(ServerBootstrap bootstrap, EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
        try {
            doConnect(bootstrap, bossGroup, workerGroup);
        } catch (InterruptedException e) {
            state = 0;
            log.error(e.getMessage(), e);
        } finally {
            //关闭管道
            closeChannel();
            //关闭线程组
            log.info("[netty server id:{}] 服务端端关闭线程组", serverId);
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private void stop0() {
        log.info("[netty server id:{}] 服务关闭中....{}", serverId, address);
        state = 0;
        mainThread.interrupt();
        // channel.close();
    }

    @Override
    public void start() {
        log.info("[netty server id:{}] start netty server {}", serverId, address);
        start0();
    }

    @Override
    public void restart() {
        log.info("[netty server id:{}] 重启服务端 restart netty server {}", serverId, address);
        start0();
    }

    @Override
    public void stop() {
        stop0();
    }


    public int state() {
        return state;
    }

    public long getServerId() {
        return serverId;
    }

    //增加处理器
    public BizNettyServer addHandler(ChannelHandler handler, String name) {
        handlers.put(handler, name);
        if (handler instanceof AbsServerHandler) {
            ((AbsServerHandler) handler).setServer(this);
        }
        return this;
    }


    //增加监听器
    public <F extends Future<?>> BizNettyServer addListener(GenericFutureListener<F> listener) {
        listeners.add(listener);
        if (listener instanceof ServerReconnectFutureListener) {
            ((ServerReconnectFutureListener) listener).setServer(this);
        }
        return this;
    }
}
