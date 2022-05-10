package com.lingfeng.rpc.server;

import com.lingfeng.rpc.model.Address;
import com.lingfeng.rpc.trans.BizDecoder;
import com.lingfeng.rpc.trans.BizEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Thread.State.TERMINATED;

/**
 * @Author: wz
 * @Date: 2022/5/7 18:26
 * @Description:
 */
@Slf4j
public class NettyServer implements Server {

    private final static AtomicInteger idStore = new AtomicInteger(0);

    private final static String PREFIX = "Biz-NettyServer:";
    //服务线程
    private volatile Thread mainThread;
    //服务地址
    private volatile Address address;
    //服务id
    private final int serverId = idStore.addAndGet(1);
    //服务状态
    private volatile int state = 0;//0 close 1 run 2 idle
    //任务处理器
    private volatile ChannelInboundHandlerAdapter serverHandler;
    //重试次数
    private volatile int retryCount = 5;

    private volatile int retryIntervalMs = 2000;//ms 重试一次

    private static final NettyServer instance = new NettyServer();

    public static NettyServer getInstance() {
        return instance;
    }

    public synchronized void start0() {
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
        //设置serverId
        if (serverHandler instanceof MyServerHandler) {
            ((MyServerHandler) serverHandler).setServerId(serverId);
        }

        mainThread = new Thread(() -> {
            //创建两个线程组 boosGroup、workerGroup
            EventLoopGroup bossGroup = new NioEventLoopGroup();
            EventLoopGroup workerGroup = new NioEventLoopGroup();
            try {
                //创建服务端的启动对象，设置参数
                ServerBootstrap bootstrap = new ServerBootstrap();
                //设置两个线程组boosGroup和workerGroup
                bootstrap.group(bossGroup, workerGroup)
                        //设置服务端通道实现类型
                        .channel(NioServerSocketChannel.class)
                        //设置线程队列得到连接个数
                        .option(ChannelOption.SO_BACKLOG, 128)
                        //设置保持活动连接状态
                        .childOption(ChannelOption.SO_KEEPALIVE, true)
                        //使用匿名内部类的形式初始化通道对象
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel socketChannel) throws Exception {
                                //给pipeline管道设置处理器
                                //new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE,12,4,0,0)
                                ChannelPipeline pipeline = socketChannel.pipeline();
                                pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 1, 4, 0, 0));
                                pipeline.addLast(new BizDecoder());
                                pipeline.addLast(new BizEncoder());
                                pipeline.addLast(serverHandler);
                            }
                        });//给workerGroup的EventLoop对应的管道设置处理器
                log.info("[netty server id:{}] 服务端已经准备就绪... {}", serverId, address);
                //绑定端口号，启动服务端
                ChannelFuture channelFuture = bootstrap.bind(address.getHost(), address.getPort()).sync();
                log.info("[netty server id:{}] 服务端开启，监听地址 {}", serverId, address);
                state = 1;
                //对关闭通道进行监听
                channelFuture.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                state = 0;
                log.error(e.getMessage(), e);
            } finally {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        });
        if (mainThread != null) {
            mainThread.setDaemon(true);
            mainThread.start();
            mainThread.setName(PREFIX + ":" + mainThread.getId());
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

    public NettyServer setAddress(Address address) {
        this.address = address;
        return this;
    }

    public NettyServer setServerHandler(ChannelInboundHandlerAdapter serverHandler) {
        this.serverHandler = serverHandler;
        return this;
    }
}
