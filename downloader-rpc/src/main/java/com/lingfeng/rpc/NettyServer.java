package com.lingfeng.rpc;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: wz
 * @Date: 2022/5/7 18:26
 * @Description:
 */
@Slf4j
public class NettyServer {
    private static final NettyServer instance = new NettyServer();

    public static NettyServer getInstance() {
        return instance;
    }

    public void start(String host, int port) {

        Thread thread = new Thread(() -> {
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
                                socketChannel.pipeline().addLast(new MyServerHandler());
                            }
                        });//给workerGroup的EventLoop对应的管道设置处理器
                log.info("java技术爱好者的服务端已经准备就绪...");
                //绑定端口号，启动服务端
                ChannelFuture channelFuture = bootstrap.bind(host,port).sync();
                //对关闭通道进行监听
                channelFuture.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
            } finally {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }
}
