package com.lingfeng.rpc.client;


import com.lingfeng.rpc.model.Address;
import com.lingfeng.rpc.trans.BizDecoder;
import com.lingfeng.rpc.trans.BizEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.lingfeng.rpc.trans.BizDecoder.*;
import static java.lang.Thread.State.TERMINATED;

/**
 * @Author: wz
 * @Date: 2022/5/7 18:26
 * @Description:
 */
@Slf4j
public class NettyClient implements Client {
    private final static AtomicInteger idStore = new AtomicInteger(0);

    private final static String PREFIX = "Biz-NettyClient:";
    //服务线程
    private volatile Thread mainThread;
    //服务地址
    private volatile Address address;
    //id
    private final int clientId = idStore.addAndGet(1);
    //服务状态
    private volatile int state = 0;//0 close 1 run 2 idle

    //重试次数
    private volatile int retryCount = 5;

    private volatile int retryIntervalMs = 2000;//ms 重试一次

    private volatile ChannelFutureListener futureListener;

    private volatile ChannelInboundHandlerAdapter handlerAdapter;

    private static final NettyClient instance = new NettyClient();

    public static NettyClient getInstance() {
        return instance;
    }

    private volatile Channel channel;

    private synchronized void start0() {
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
        //设置 clientId
        if (handlerAdapter instanceof MyClientHandler) {
            MyClientHandler handler = ((MyClientHandler) handlerAdapter);
            handler.setClientId(clientId).setClient(this);
        }

        //设置 clientId
        if (futureListener instanceof MyChannelFutureListener) {
            MyChannelFutureListener listener = ((MyChannelFutureListener) futureListener);
            (listener).setClientId(clientId).setClient(this);
        }
        mainThread = new Thread(() -> {
            NioEventLoopGroup eventExecutors = new NioEventLoopGroup();
            try {
                //创建bootstrap对象，配置参数
                Bootstrap bootstrap = new Bootstrap();
                //设置线程组
                bootstrap.group(eventExecutors)
                        //设置客户端的通道实现类型
                        .channel(NioSocketChannel.class)
                        //使用匿名内部类初始化通道
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) throws Exception {
                                ChannelPipeline pipeline = ch.pipeline();
                                pipeline.addLast(new LengthFieldBasedFrameDecoder(MAXFRAMELENGTH, LENGTHFIELDOFFSET, LENGTHFIELDLENGTH, LENGTHADJUSTMENT, INITIALBYTESTOSTRIP));
                                pipeline.addLast(new BizDecoder());
                                pipeline.addLast(new BizEncoder());
                                //添加客户端通道的处理器
                                ch.pipeline().addLast(handlerAdapter);
                            }
                        });
                //连接服务端
                ChannelFuture channelFuture = bootstrap.connect(address.getHost(), address.getPort()).sync();
                //注册监听者
                channelFuture.addListener(futureListener);
                log.info("[netty client id:{}] 客户端启动成功！", clientId);
                state = 1;
                //channel
                channel = channelFuture.channel();
                //对通道关闭进行监听
                channelFuture.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                state = 0;
                log.error(e.getMessage(), e);
            } finally {
                //关闭管道
                closeChannel();
                //关闭线程组
                log.info("[netty client id:{}] 客户端关闭线程组", clientId);
                eventExecutors.shutdownGracefully();
            }
        });
        if (mainThread != null) {
            mainThread.setPriority(Thread.MAX_PRIORITY);
            mainThread.setDaemon(true);
            mainThread.setName(PREFIX + ":" + mainThread.getId());
            mainThread.start();
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

    @Override
    public void restart() {
        log.info("[netty client id: {}] === restart ===", clientId);
        do {
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            start();
        } while (state() != State.RUNNING.getCode());
    }

    @Override
    public void close() {
        close0();
    }

    private void closeChannel() {
        channel.close();
    }

    public NettyClient setAddress(Address address) {
        this.address = address;
        return this;
    }

    public NettyClient setListener(ChannelFutureListener futureListener) {
        this.futureListener = futureListener;
        return this;
    }

    public NettyClient setHandler(ChannelInboundHandlerAdapter handlerAdapter) {
        this.handlerAdapter = handlerAdapter;
        return this;
    }

    public int getClientId() {
        return clientId;
    }

    public Channel getChannel() {
        return channel;
    }
}
