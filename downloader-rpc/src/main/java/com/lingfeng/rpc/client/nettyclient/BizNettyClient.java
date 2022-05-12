package com.lingfeng.rpc.client.nettyclient;


import cn.hutool.core.thread.ThreadFactoryBuilder;
import com.lingfeng.rpc.client.handler.AbsClientHandler;
import com.lingfeng.rpc.client.handler.ReConnectFutureListener;
import com.lingfeng.rpc.constant.Cmd;
import com.lingfeng.rpc.constant.State;
import com.lingfeng.rpc.model.Address;
import com.lingfeng.rpc.util.SystemClock;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static java.lang.Thread.State.TERMINATED;

/**
 * @Author: wz
 * @Date: 2022/5/7 18:26
 * @Description:
 */
@Slf4j
public class BizNettyClient extends AbsNettyClient implements NettyClient {

    private final static String THREAD_PREFIX = "Biz-NettyClient-Thread";
    //最大重启次数
    private static final int MAX_RESTART_TIME = 60000; //60*1000 1分钟内尝试重启60次

    private volatile Thread mainThread = null;


    private volatile NioEventLoopGroup loopGroup;


    //simple thread model 内置一个守护线程发送数据
    private synchronized void start0(boolean isReload) {
        Address address = getAddress();
        log.info("[netty client id:{}] start0", clientId);
        if (mainThread != null) {
            //只有关闭的状态才能进行启动
            if (state != State.CLOSED.code()) {
                log.error("[netty client id:{}]！客户端启动失败 address={} state={}", clientId, address, State.trans(state).name());
                return;
                //关闭的状态但是主线程状态不是关闭态
            } else if (state == State.CLOSED.code() && !mainThread.getState().equals(TERMINATED)) {
                log.error("[netty client id:{}] 客户端启动失败 client mainThread 状态非停止！{}", clientId, mainThread.getState());
                return;
                //启动中的
            } else if (state == State.STARTING.code()) {
                log.warn("[netty client id:{}] 客户端启动中...", clientId);
                return;
            }
        }
        //标明已经有线程在启动了
        state = State.STARTING.code();

        mainThread = new Thread(() -> {
            if (loopGroup == null || loopGroup.isShutdown() || loopGroup.isShuttingDown() || loopGroup.isTerminated()) {
                loopGroup = new NioEventLoopGroup();
            }
            //创建bootstrap对象，配置参数
            connectHandler(loopGroup);
        });
        if (mainThread != null) {
            mainThread.setPriority(Thread.MAX_PRIORITY);
            mainThread.setDaemon(true);
            mainThread.setName(THREAD_PREFIX + ":" + mainThread.getId());
            mainThread.start();
        }
    }

    private void connectHandler(EventLoopGroup eventLoopGroup) {
        try {
            Bootstrap bootstrap = new Bootstrap();
            doConnect(bootstrap, eventLoopGroup);
            //这里会阻塞
            // channelFuture.sync();
            log.info("doConnect");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            //如果是关闭并且不重启
            if (state == State.CLOSED_NO_RETRY.code()) {
                giveUp();
            } else {
                //关闭态
                state = 0;
                closeChannel();
                closeThreadGroup();
                //进入重试
                restart();
            }
        }
    }

    //放弃重试
    private void giveUp() {
        //将状态设置为关闭并不重启
        state = State.CLOSED_NO_RETRY.code();
        closeChannel();
        //关闭线程组
        closeThreadGroup();
        loopGroup = null;
    }

    private void closeThreadGroup() {
        //关闭线程组
        log.info("[netty client id:{}] 客户端关闭线程组", clientId);
        loopGroup.shutdownGracefully();
    }

    private void close0() {
        log.info("[netty client id:{}] 客户端关闭中....{}", clientId, address);
        state = State.CLOSED_NO_RETRY.code();
        mainThread.interrupt();
    }

    @Override
    public int state() {
        return state;
    }


    @Override
    public void start() {
        start0(false);
    }

    @Override
    public void restart(long totalTime, TimeUnit unit, long pertime) {
        log.info("[netty client id: {}] === restart ===", clientId);
        restartLoop(totalTime, unit, pertime);
    }

    @Override
    public void restart() {
        log.info("[netty client id: {}] === restart ===", clientId);
        restartLoop(MAX_RESTART_TIME, TimeUnit.MILLISECONDS, 1000);
    }


    private volatile Thread restartThread = null;
    private final Object restartLock = new Object();

    private void restartLoop(long totalTime, TimeUnit unit, long time) {
        synchronized (restartLock) {
            if (restartThread != null) {
                return;
            }
            log.info("[netty client id: {}] 开始重启", getClientId());
            restartThread = new Thread(() -> {
                long start = SystemClock.now();
                while (state() != State.RUNNING.code() && (SystemClock.now() - start) < totalTime) {
                    start0(true);
                    log.info("restartThread");
                    try {
                        unit.sleep(time);
                        Channel channel = getChannel();
                        if (channel != null && channel.isOpen()) {
                            log.info("发送 test77788");
                            writeAndFlush(channel, "test77788", Cmd.HEARTBEAT);
                        }
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
                restartThread = null;
            });
            restartThread.start();
        }
    }


    @Override
    public void close() {
        close0();
    }


    protected void closeChannel() {
        if (channel != null) {
            try {
                channel.close();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    //增加处理器
    public BizNettyClient addHandler(ChannelHandler handler, String name) {
        handlers.add(handler);
        if (handler instanceof AbsClientHandler) {
            ((AbsClientHandler) handler).setClient(this);
        }
        return this;
    }


    //增加监听器
    public <F extends Future<?>> BizNettyClient addListener(GenericFutureListener<F> listener) {
        listeners.add(listener);
        if (listener instanceof ReConnectFutureListener) {
            ((ReConnectFutureListener) listener).setClient(this);
        }
        return this;
    }
}
