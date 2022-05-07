package com.lingfeng.rpc;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: wz
 * @Date: 2022/5/7 18:26
 * @Description:
 */
@Slf4j
public class NettyClient {

    private static final NettyClient instance = new NettyClient();

    public static NettyClient getInstance() {
        return instance;
    }


    public void start(String host, int port) {

        Thread thread = new Thread(() -> {
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
                                //添加客户端通道的处理器
                                ch.pipeline().addLast(MyClientHandler.getInstance());
                            }
                        });
                log.info("客户端准备就绪，随时可以起飞~");
                //连接服务端
                ChannelFuture channelFuture = bootstrap.connect(host, port).sync();
                //对通道关闭进行监听
                channelFuture.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
            } finally {
                //关闭线程组
                eventExecutors.shutdownGracefully();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }
}
