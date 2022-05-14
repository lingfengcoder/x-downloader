package com.lingfeng.rpc;


import cn.hutool.core.util.RandomUtil;
import com.lingfeng.rpc.constant.Cmd;
import com.lingfeng.rpc.data.Frame;
import com.lingfeng.rpc.handler.NettyServerHandler;
import com.lingfeng.rpc.model.Address;
import com.lingfeng.rpc.server.nettyserver.BizNettyServer;
import com.lingfeng.rpc.server.nettyserver.NettyServerFactory;
import io.netty.channel.Channel;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


@Component
public class RpcServer {
    AtomicInteger data = new AtomicInteger(RandomUtil.randomInt(90, 1000));

    @PostConstruct
    public void init() {
        BizNettyServer server = NettyServerFactory.buildBizNettyServer(new Address("127.0.0.1", 9999), () -> Arrays.asList(new NettyServerHandler()));
        server.start();


        new Thread(() -> {
            while (true) {
                try {
                    // TimeUnit.SECONDS.sleep(5);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                int i = data.incrementAndGet();
                Collection<Channel> channels = server.allChannels();
                for (Channel channel : channels) {
                    testString(server, channel);
                }
            }
        }).start();
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
