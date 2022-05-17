package com.lingfeng.rpc.server.nettyserver;


import com.lingfeng.rpc.constant.Cmd;
import com.lingfeng.rpc.constant.State;
import com.lingfeng.rpc.model.Address;
import com.lingfeng.rpc.server.handler.AbsServerHandler;
import com.lingfeng.rpc.server.listener.ServerReconnectFutureListener;
import com.lingfeng.rpc.trans.MessageTrans;
import com.lingfeng.rpc.util.SnowFlake;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.channel.group.ChannelMatcher;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;


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
    protected volatile int state = 0;//State.class
    //处理器集合
    protected volatile List<ChannelHandler> handlers = new ArrayList<>();
    //监听器集合
    protected volatile List<GenericFutureListener<? extends Future<?>>> listeners = new ArrayList<>();

    protected volatile Consumer<AbsNettyServer> configFunction;

    protected volatile Channel defaultChannel;

    protected volatile ChannelHandlerContext defaultContext;

    //连接通道的集合
    protected final ConcurrentHashMap<String, Channel> channels = new ConcurrentHashMap<>();

    @Override
    public void showChannels() {
        log.info("[当前服务器中存活的channel] {}", channels);
    }

    @Override
    public Collection<Channel> allChannels() {
        return this.channels.values();
    }

    @Override
    public void closeChannel(String clientId) {
        Channel channel = channels.get(clientId);
        if (channel != null) {
            log.info(" [closeChannel] 关闭  channel {}", clientId);
            channel.close();
            channels.remove(clientId);
        } else {
            log.info("channel 不在缓存中 clientId={}", clientId);
        }
    }

    //关闭所有的连接
    protected void closeAllChannel() {
        for (Channel channel : channels.values()) {
            channel.close();
        }
    }

    @Override
    public void addChannel(String clientId, Channel channel) {
        if (!channels.containsKey(clientId)) {
            channels.put(clientId, channel);
        }
    }

    @Override
    public Channel findChanel(String clientId) {
        return channels.get(clientId);
    }

    //@Override
    public void config(Consumer<AbsNettyServer> config) {
        this.configFunction = config;
    }

    /**
     * netty server 连接，连接失败5秒后重试连接
     */
    public ChannelFuture doConnect(ServerBootstrap bootstrap, EventLoopGroup bossGroup, EventLoopGroup workerGroup) throws InterruptedException {
        if (bootstrap != null) {
            AbsNettyServer that = this;
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
                            log.info("initChannel ========= {}", socketChannel.hashCode());
                            if (configFunction != null) {
                                log.info(" configFunction! = null");
                                handlers.clear();
                                listeners.clear();
                                configFunction.accept(that);
                            }
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            for (ChannelHandler handler : handlers) {
                                pipeline.addLast(handler);
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
            state = State.RUNNING.code();
            //对关闭通道进行监听
            return channelFuture.channel().closeFuture();
            //.sync();
        }
        return null;
    }

    public int state() {
        return state;
    }

    public long getServerId() {
        return serverId;
    }

    protected void closeChannel() {
        if (defaultChannel != null) {
            defaultChannel.close();
        }
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


    public <M extends Serializable> void writeAndFlush(ChannelHandlerContext ctx, M msg, Cmd type) {
        Channel channel = ctx.channel();
        if (!channel.isActive() && channel.isOpen()) {
            throw new RuntimeException("[server] channel is not open or active！");
        }
        //如果channel没有注册好 则循环等待
        accessClientState();
        // log.info("ctx hashCode={} [write]", channel.hashCode());
        switch (type) {
            case HEARTBEAT:
                ctx.writeAndFlush(MessageTrans.heartbeatFrame(getServerId()));
                break;
            case REQUEST:
                ctx.writeAndFlush(MessageTrans.dataFrame(msg, Cmd.REQUEST, getServerId()));
                break;
            case RESPONSE:
                ctx.writeAndFlush(MessageTrans.dataFrame(msg, Cmd.RESPONSE, getServerId()));
                break;
        }
    }

    public <M extends Serializable> void writeAndFlush(Channel channel, M msg, Cmd type) {
        if (!channel.isActive() && channel.isOpen()) {
            throw new RuntimeException(" channel is not open or active！");
            // return;
        }
        //如果channel没有注册好 则循环等待
        accessClientState();
        // log.info("ctx hashCode={} [write]", channel.hashCode());
        switch (type) {
            case HEARTBEAT:
                channel.writeAndFlush(MessageTrans.heartbeatFrame(getServerId()));
                break;
            case REQUEST:
                //ByteBuf buf = Unpooled.copiedBuffer("data", CharsetUtil.UTF_8);
                channel.writeAndFlush(MessageTrans.dataFrame(msg, Cmd.REQUEST, getServerId()));
                break;
            case RESPONSE:
                channel.writeAndFlush(MessageTrans.dataFrame(msg, Cmd.RESPONSE, getServerId()));
                break;
        }
    }


    //判断client的状态，如果已经关闭
    private void accessClientState() {
        // boolean removed = channel.isRemoved();
        if (State.RUNNING.code() != state) {
            throw new RuntimeException("[netty server id: " + this.getServerId() + "] client state error, state=" + State.trans(state));
        }
    }

    //增加处理器
    // @Override
    public AbsNettyServer addHandler(ChannelHandler handler) {
        handlers.add(handler);
        if (handler instanceof AbsServerHandler) {
            ((AbsServerHandler) handler).setServer(this);
        }
        return this;
    }

    //增加监听器
    public <F extends Future<?>> AbsNettyServer addListener(GenericFutureListener<F> listener) {
        listeners.add(listener);
        if (listener instanceof ServerReconnectFutureListener) {
            ((ServerReconnectFutureListener) listener).setServer(this);
        }
        return this;
    }

    @Override
    public void setDefaultChannelContext(ChannelHandlerContext channelContext) {
        this.defaultContext = channelContext;
    }

    @Override
    public ChannelHandlerContext getDefaultChannelContext() {
        return this.defaultContext;
    }




}
