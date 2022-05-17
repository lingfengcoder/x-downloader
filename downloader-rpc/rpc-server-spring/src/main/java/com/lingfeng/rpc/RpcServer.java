package com.lingfeng.rpc;


import cn.hutool.core.util.RandomUtil;
import com.lingfeng.rpc.constant.Cmd;
import com.lingfeng.rpc.data.Frame;
import com.lingfeng.rpc.handler.NettyServerHandler;
import com.lingfeng.rpc.model.Address;
import com.lingfeng.rpc.server.nettyserver.BizNettyServer;
import com.lingfeng.rpc.server.nettyserver.NettyServerFactory;
import com.lingfeng.rpc.util.SystemClock;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


@Component
public class RpcServer {
    private final AtomicInteger data = new AtomicInteger();

    @PostConstruct
    public void init() {
        BizNettyServer server = NettyServerFactory.buildBizNettyServer(new Address("127.0.0.1", 9999), () -> Arrays.asList(new NettyServerHandler()));
        server.start();


        new Thread(() -> {
            long now = SystemClock.now();
            while (true) {
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Collection<Channel> channels = server.allChannels();
                if (!channels.isEmpty()) {
                    // testStringByCTX(server);
                }
                for (Channel channel : channels) {
                    testString(server, channel);
                }
            }
        }).start();

        new Thread(() -> {
            while (true) {
                System.out.println(" data = " + data.get());
                try {
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    private void testStringByCTX(BizNettyServer server) {
        ChannelHandlerContext defaultChannelContext = server.getDefaultChannelContext();
        Frame<Object> frame = new Frame<>();
        frame.setTarget("bbq");
        frame.setData(" clientId = " + data.incrementAndGet());
        if (defaultChannelContext != null) {
            server.writeAndFlush(defaultChannelContext, frame, Cmd.REQUEST);
        }
    }

    private void testString(BizNettyServer server, Channel channel) {
        Frame<Object> frame = new Frame<>();
        frame.setTarget("bbq");
        String clientId = channel.id().asLongText();
        frame.setData(" clientId = " + data.incrementAndGet());
        server.writeAndFlush(channel, frame, Cmd.REQUEST);
    }

    private void testComplex(BizNettyServer server, Channel channel) {
        Frame<Object> frame = new Frame<>();
        frame.setTarget("complexParam");
        String name = channel.id().asLongText();
        HashMap<Object, Object> param = new HashMap<>();
        int x = RandomUtil.randomInt(1, 20);
        for (int y = 0; y < x; y++) {
            param.put("k:" + y, y);
        }
        frame.setData(param);
        server.writeAndFlush(channel, frame, Cmd.REQUEST);
    }
}
