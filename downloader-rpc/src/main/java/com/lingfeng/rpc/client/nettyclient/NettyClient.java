package com.lingfeng.rpc.client.nettyclient;


import com.lingfeng.rpc.client.handler.BaseClientHandler;
import com.lingfeng.rpc.client.handler.ReConnectFutureListener;
import com.lingfeng.rpc.constant.State;
import com.lingfeng.rpc.model.Address;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
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
public class NettyClient extends BaseNettyClient implements Client {

    private final static String THREAD_PREFIX = "Biz-NettyClient-Thread";

    private volatile Thread mainThread = null;

    //simple thread model 内置一个守护线程发送数据
    private synchronized void start0() {
        Address address = getAddress();
        log.info("[netty client id:{}] start0", clientId);
        if (mainThread != null) {
            if (state != 0) {
                log.error("[netty client id:{}]！客户端启动失败 address={} state={}", clientId, address, State.trans(state).name());
                return;
            } else if (state == 0 && !mainThread.getState().equals(TERMINATED)) {
                log.error("[netty client id:{}] 客户端启动失败 client mainThread 状态非停止！{}", clientId, mainThread.getState());
                return;
            }
        }
        mainThread = new Thread(() -> {
            //创建bootstrap对象，配置参数
            Bootstrap bootstrap = new Bootstrap();
            EventLoopGroup eventExecutors = new NioEventLoopGroup();
            connect(bootstrap, eventExecutors);
        });
        if (mainThread != null) {
            mainThread.setPriority(Thread.MAX_PRIORITY);
            mainThread.setDaemon(true);
            mainThread.setName(THREAD_PREFIX + ":" + mainThread.getId());
            mainThread.start();
        }
    }

    public void connect(Bootstrap bootstrap, EventLoopGroup eventLoopGroup) {
        try {
            doConnect(bootstrap, eventLoopGroup);
        } catch (InterruptedException e) {
            state = 0;
            log.error(e.getMessage(), e);
        } finally {
            //关闭管道
            closeChannel();
            //关闭线程组
            log.info("[netty client id:{}] 客户端关闭线程组", clientId);
            eventLoopGroup.shutdownGracefully();
        }
    }


    private void close0() {
        log.info("[netty client id:{}] 客户端关闭中....{}", clientId, address);
        state = 0;
        mainThread.interrupt();
    }

    @Override
    public int state() {
        return state;
    }

    @Override
    public void start() {
        start0();
    }

    //指定bootstrap的重连
    public void restart(Bootstrap bootstrap, EventLoop loopGroup) {
        log.info("[netty client id: {}] === restart ===", clientId);
        connect(bootstrap, loopGroup);
    }

    public void restart() {
        log.info("[netty client id: {}] === restart ===", clientId);

    }

    @Override
    public void close() {
        close0();
    }

    protected void closeChannel() {
        channel.close();
    }

    //增加处理器
    public NettyClient addHandler(ChannelHandler handler, String name) {
        handlers.put(handler, name);
        if (handler instanceof BaseClientHandler) {
            ((BaseClientHandler) handler).setClient(this);
        }
        return this;
    }

    //增加监听器
    public <F extends Future<?>> NettyClient addListener(GenericFutureListener<F> listener) {
        listeners.add(listener);
        if (listener instanceof ReConnectFutureListener) {
            ((ReConnectFutureListener) listener).setClient(this);
        }
        return this;
    }
}
