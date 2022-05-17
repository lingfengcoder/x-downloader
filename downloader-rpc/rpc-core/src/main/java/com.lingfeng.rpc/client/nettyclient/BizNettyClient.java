package com.lingfeng.rpc.client.nettyclient;


import com.lingfeng.rpc.constant.State;
import com.lingfeng.rpc.model.Address;
import com.lingfeng.rpc.util.SystemClock;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

/**
 * @Author: wz
 * @Date: 2022/5/7 18:26
 * @Description:
 */
@Slf4j
public class BizNettyClient extends AbsNettyClient implements NettyClient {

    //最大重启次数
    private static final int MAX_RESTART_TIME = 60000; //60*1000 1分钟内尝试重启60次
    //开始重启的时间点
    private static volatile long restartBeginTime = 0;
    //客户端读写线程池
    private volatile NioEventLoopGroup loopGroup;
    //  主动中断使用的线程  private volatile Thread mainThread = null;

    //只有一个线程在运行，形成客户端阻塞的状态,避免循环启动客户端
    private final ScheduledExecutorService scheduled = new ScheduledThreadPoolExecutor(1, new ThreadPoolExecutor.AbortPolicy());
    private volatile ScheduledFuture<?> scheduledFuture = null;

    //simple thread model 内置一个守护线程发送数据
    private synchronized void start0() {
        log.info("[netty client id:{}] start0", clientId);
        if (scheduledFuture != null && !scheduledFuture.isCancelled()) {
            log.info("client线程已经在执行了");
            return;
        }
        //客户端的守护线程
        Runnable mainTask = mainTask();
        scheduledFuture = scheduled.scheduleAtFixedRate(mainTask, 0, 1000, TimeUnit.MILLISECONDS);
    }

    //状态控制器
    private boolean stateCtrl() {
        //只有关闭的状态才能进行启动
        if (state != State.CLOSED.code()) {
            if (state == State.STARTING.code()) {
                log.warn("[netty client id:{}] 客户端启动中...", clientId);
                return false;
            } else if (state == State.CLOSED_NO_RETRY.code()) {
                log.warn("[netty client id:{}] 客户端已经进入了 [关闭并不重启] 的状态,放弃重启...", clientId);
                giveUpRestart();
                return false;
            }
            Address address = getAddress();
            log.error("[netty client id:{}]！客户端启动失败 address={} state={}", clientId, address, State.trans(state).name());
            return false;
        }
        return true;
    }


    private Runnable mainTask() {
        return () -> {
            if (!stateCtrl()) return;
            //标明客户端正在启动
            state = State.STARTING.code();
            //不建议复用NioEventLoopGroup 可能会造成大量的线程不能释放
            if (loopGroup == null || loopGroup.isShutdown() || loopGroup.isShuttingDown() || loopGroup.isTerminated()) {
                loopGroup = new NioEventLoopGroup();
            }
            //创建bootstrap对象，配置参数
            try {
                log.info("doConnect");
                Bootstrap bootstrap = new Bootstrap();
                ChannelFuture channelFuture = doConnect(bootstrap, loopGroup);
                //启动成功后，重置时间
                restartBeginTime = 0;
                // 可以主动中断 mainThread = Thread.currentThread();
                //阻塞 wait
                channelFuture.sync();
                log.info("lost connect");
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            } finally {
                //如果是关闭并且不重启，放弃重启
                if (state == State.CLOSED_NO_RETRY.code()) {
                    giveUpRestart();
                } else {
                    //如果是从正常运行 突然关闭，那么就转换为关闭态
                    //关闭态
                    state = State.CLOSED.code();
                    closeChannel();
                    //关闭线程组
                    closeThreadGroup();
                    //进入重启
                    enterRetry();
                }
            }
        };
    }


    //进入重启循环
    private void enterRetry() {
        //说明是从正常启动变为异常，进入重试流程
        if (restartBeginTime == 0) {
            restartBeginTime = SystemClock.now();
        }
        //如果超时,关闭定时任务
        if (restartBeginTime > 0) {
            if (SystemClock.now() - restartBeginTime > MAX_RESTART_TIME) {
                giveUpRestart();
            }
        }
    }

    //放弃重试
    private void giveUpRestart() {
        //将状态设置为关闭并不重启
        state = State.CLOSED_NO_RETRY.code();
        closeChannel();
        //关闭线程组
        closeThreadGroup();
        //关闭循环
        cancelSchedule();
        loopGroup = null;
        handlers.clear();
        listeners.clear();
        channel = null;
    }

    private void cancelSchedule() {
        //取消定时
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }
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

    private void closeThreadGroup() {
        //关闭线程组
        log.info("[netty client id:{}] 客户端关闭线程组", clientId);
        if (loopGroup != null) {
            loopGroup.shutdownGracefully();
        }
    }

    private void close0() {
        log.info("[netty client id:{}] 客户端关闭中....{}", clientId, address);
        //将状态设置为关闭并不重启
        giveUpRestart();
    }

    @Override
    public int state() {
        return state;
    }

    @Override
    public void start() {
        start0();
    }

    @Override
    public void restart() {
        //重置 结束时间并开启
        log.info("[netty client id: {}]  restart ", clientId);
        //重置重试时间
        restartBeginTime = 0;
        //为了避免schedule已经停止，主动再次
        start0();
    }

    @Override
    public void close() {
        close0();
    }

}
