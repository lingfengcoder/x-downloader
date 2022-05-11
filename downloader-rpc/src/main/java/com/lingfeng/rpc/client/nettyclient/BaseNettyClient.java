package com.lingfeng.rpc.client.nettyclient;


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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @Author: wz
 * @Date: 2022/5/7 18:26
 * @Description:
 */
@Slf4j
public abstract class BaseNettyClient implements Client {

    //服务地址
    protected volatile Address address;
    //id
    protected volatile long clientId = SnowFlake.next();
    //服务状态
    protected volatile int state = 0;//0 close 1 run 2 idle
    //处理器集合
    protected final Map<ChannelHandler, String> handlers = new HashMap<>();
    //监听器集合
    protected volatile List<GenericFutureListener<? extends Future<?>>> listeners = new ArrayList<>();

    protected volatile Channel channel;


    /**
     * netty client 连接，连接失败5秒后重试连接
     */
    public Bootstrap doConnect(Bootstrap bootstrap, EventLoopGroup eventLoopGroup) throws InterruptedException {
        if (bootstrap != null) {
            //设置线程组
            bootstrap.group(eventLoopGroup)
                    //设置客户端的通道实现类型
                    .channel(NioSocketChannel.class)
                    //使用匿名内部类初始化通道
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            for (Map.Entry<ChannelHandler, String> item : handlers.entrySet()) {
                                String name = item.getValue();
                                if (StringUtils.isNotEmpty(name)) {
                                    pipeline.addLast(name, item.getKey());
                                } else {
                                    pipeline.addLast(item.getKey());
                                }
                            }
                        }
                    });
            //连接服务端
            ChannelFuture channelFuture =
                    bootstrap.connect(address.getHost(), address.getPort()).sync();
            //注册监听者
            for (GenericFutureListener listener : listeners) {
                channelFuture.addListener(listener);
            }
            log.info("[netty client id:{}] 客户端启动成功！", clientId);
            state = 1;
            //channel
            channel = channelFuture.channel();
            //对通道关闭进行监听
            channelFuture.channel().closeFuture().sync();
        }
        return bootstrap;
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


    public <M extends Serializable> void writeAndFlush(M msg, Cmd type) {
        //如果channel没有注册好 则循环等待
        accessChannel();
        accessClientState();
        log.info("ctx hashCode={} [write]", channel.hashCode());
        switch (type) {
            case HEARTBEAT:
                channel.writeAndFlush(MessageTrans.heartbeatFrame(getClientId()));
                break;
            case REQUEST:
                channel.writeAndFlush(MessageTrans.dataFrame(msg, getClientId()));
                break;
        }
    }

    //判断client的状态，如果已经关闭
    private void accessClientState() {
        // boolean removed = channel.isRemoved();
        if (State.RUNNING.getCode() != state) {
            throw new RuntimeException("[netty client id: " + this.getClientId() + "] client state error, state=" + state);
        }
    }

    //如果channel没有注册好 则循环等待
    private void accessChannel() {
        while (channel == null) {
            try {
                log.info("wait for channel");
                TimeUnit.MILLISECONDS.sleep(200);
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

}
