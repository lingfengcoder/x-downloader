package com.lingfeng.rpc.client.nettyclient;


import com.lingfeng.rpc.coder.Coder;
import com.lingfeng.rpc.coder.CoderFactory;
import com.lingfeng.rpc.coder.safe.SafeCoder;
import com.lingfeng.rpc.constant.Cmd;
import com.lingfeng.rpc.constant.State;
import com.lingfeng.rpc.model.Address;
import com.lingfeng.rpc.trans.MessageTrans;
import com.lingfeng.rpc.util.SnowFlake;
import com.lingfeng.rpc.util.StringUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @Author: wz
 * @Date: 2022/5/7 18:26
 * @Description:
 */
@Slf4j
public abstract class AbsNettyClient implements NettyClient {

    //服务地址
    protected volatile Address address;
    //id
    protected volatile long clientId = SnowFlake.next();
    //服务状态
    protected volatile int state = 0;//0 close 1 run 2 starting
    //处理器集合
    protected final List<ChannelHandler> handlers = new ArrayList<>();

    //监听器集合
    protected volatile List<GenericFutureListener<? extends Future<?>>> listeners = new ArrayList<>();

    private volatile Consumer<NettyClient> configFunction;
    protected volatile Channel channel;

    @Override
    public void config(Consumer<NettyClient> consumer) {
        this.configFunction = consumer;
    }

    @Override
    public void defaultChannel(Channel channel) {
        this.channel = channel;
    }


    public ChannelFuture doConnect(Bootstrap bootstrap, EventLoopGroup eventLoopGroup) throws InterruptedException {
        if (bootstrap != null) {
            //触发配置 保证每次初始化都是新的对象
            handlers.clear();
            listeners.clear();
            configFunction.accept(this);
            //设置线程组
            bootstrap.group(eventLoopGroup)
                    //设置客户端的通道实现类型
                    .channel(NioSocketChannel.class)
                    //使用匿名内部类初始化通道
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            //注意添加顺序
                            for (ChannelHandler handler : handlers) {
                                pipeline.addLast(handler);
                            }
//                            for (Map.Entry<ChannelHandler, String> item : handlers.entrySet()) {
//                                String name = item.getValue();
//                                if (StringUtils.isNotEmpty(name)) {
//                                    pipeline.addLast(name, item.getKey());
//                                } else {
//                                    pipeline.addLast(item.getKey());
//                                }
//                            }
                        }
                    });

            ChannelFuture channelFuture =
                    bootstrap.connect(address.getHost(), address.getPort()).sync();
            //注册监听者
            for (GenericFutureListener listener : listeners) {
                channelFuture.addListener(listener);
            }
            channelFuture.addListener((ChannelFuture futureListener) -> {
                final EventLoop eventLoop = futureListener.channel().eventLoop();
                if (!futureListener.isSuccess()) {
                    log.warn("连接服务器失败，5s后重新尝试连接！");
                    //思路：线程组不关闭，Bootstrap 重新构建
                    futureListener.channel().eventLoop().schedule(() ->
                            doConnect(new Bootstrap(), eventLoop), 3, TimeUnit.SECONDS);
                }
            });
            log.info("[netty client id:{}] 客户端启动成功！", clientId);
            //channel
            channel = channelFuture.channel();
            //启动成功！
            state = State.RUNNING.code();
            //对通道关闭进行监听
            channelFuture.channel().closeFuture().sync();
        }
        return null;
    }


    protected void closeChannel() {
        channel.close();
    }

    public long getClientId() {
        return clientId;
    }

    public Channel getChannel() {
        return channel;
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
        //  log.info("ctx hashCode={} [write]", channel.hashCode());
        switch (type) {
            case HEARTBEAT:
                channel.writeAndFlush(MessageTrans.heartbeatFrame(getClientId()));
                break;
            case REQUEST:
                channel.writeAndFlush(MessageTrans.dataFrame(msg, getClientId()));
                break;
        }
    }

    public <M extends Serializable> void writeAndFlush(M msg, Cmd type) {
        accessChannel();
        writeAndFlush(channel, msg, type);
    }

    //判断client的状态，如果已经关闭
    private void accessClientState() {
        // boolean removed = channel.isRemoved();
        if (State.RUNNING.code() != state) {
            throw new RuntimeException("[netty client id: " + this.getClientId() + "] client state error, state=" + state);
        }
    }

    //如果channel没有注册好 则循环等待
    protected void accessChannel() {
        while (channel == null) {
            try {
                log.info("wait for channel");
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

}
