package com.lingfeng.rpc.server.nettyserver;


import com.lingfeng.rpc.coder.Coder;
import com.lingfeng.rpc.coder.CoderFactory;
import com.lingfeng.rpc.coder.safe.SafeCoder;
import com.lingfeng.rpc.constant.Cmd;
import com.lingfeng.rpc.constant.State;
import com.lingfeng.rpc.model.Address;
import com.lingfeng.rpc.trans.MessageTrans;
import com.lingfeng.rpc.util.SnowFlake;
import com.lingfeng.rpc.util.StringUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @Author: wz
 * @Date: 2022/5/7 18:26
 * @Description:
 */
@Slf4j
public abstract class AbsNettyServer implements NettyServer {

    //服务地址
    protected volatile Address address;
    //服务id
    protected final long serverId = SnowFlake.next();
    //服务状态
    protected volatile int state = 0;//0 close 1 run 2 idle
    //处理器集合
    protected final Map<ChannelHandler, String> handlers = new HashMap<>();
    //监听器集合
    protected volatile List<GenericFutureListener<? extends Future<?>>> listeners = new ArrayList<>();

    protected volatile Channel defaultChannel;

    /**
     * netty client 连接，连接失败5秒后重试连接
     */
    // EventLoopGroup bossGroup = new NioEventLoopGroup();
    //            EventLoopGroup workerGroup = new NioEventLoopGroup();
    public ServerBootstrap doConnect(ServerBootstrap bootstrap, EventLoopGroup bossGroup, EventLoopGroup workerGroup) throws InterruptedException {
        if (bootstrap != null) {
            //创建服务端的启动对象，设置参数
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
                            ChannelPipeline pipeline = socketChannel.pipeline();

                            CoderFactory coderFactory = CoderFactory.getInstance();
                            Coder generate = coderFactory.generate(SafeCoder.class);
                            //自定义传输协议 coder 不允许共享 每次只能重新new
                            pipeline.addLast(generate.type());
                            pipeline.addLast(generate.decode());
                            pipeline.addLast(generate.encode());

                            for (Map.Entry<ChannelHandler, String> item : handlers.entrySet()) {
                                String name = item.getValue();
                                if (StringUtils.isNotEmpty(name)) {
                                    pipeline.addLast(name, item.getKey());
                                } else {
                                    pipeline.addLast(item.getKey());
                                }
                            }
                        }
                    });//给workerGroup的EventLoop对应的管道设置处理器
            log.info("[netty server id:{}] 服务端已经准备就绪... {}", serverId, address);
            //绑定端口号，启动服务端
            ChannelFuture channelFuture = bootstrap.bind(address.getHost(), address.getPort()).sync();
            log.info("[netty server id:{}] 服务端开启，监听地址 {}", serverId, address);
            //注册监听者
            for (GenericFutureListener listener : listeners) {
                channelFuture.addListener(listener);
            }
            //channel
            defaultChannel = channelFuture.channel();
            state = 1;
            //对关闭通道进行监听
            channelFuture.channel().closeFuture().sync();
        }
        return bootstrap;
    }

    public int state() {
        return state;
    }

    public long getServerId() {
        return serverId;
    }

    protected void closeChannel() {
        defaultChannel.close();
    }

    public Channel getChannel() {
        return defaultChannel;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }


    public <M extends Serializable> void writeAndFlush(Channel channel, M msg, Cmd type) {
        //如果channel没有注册好 则循环等待
        accessClientState();
        // log.info("ctx hashCode={} [write]", channel.hashCode());
        switch (type) {
            case HEARTBEAT:
                channel.writeAndFlush(MessageTrans.heartbeatFrame(getServerId()));
                break;
            case REQUEST:
                channel.writeAndFlush(MessageTrans.dataFrame(msg, getServerId()));
                break;
        }
    }

    //判断client的状态，如果已经关闭
    private void accessClientState() {
        // boolean removed = channel.isRemoved();
        if (State.RUNNING.code() != state) {
            throw new RuntimeException("[netty server id: " + this.getServerId() + "] client state error, state=" + state);
        }
    }

}
