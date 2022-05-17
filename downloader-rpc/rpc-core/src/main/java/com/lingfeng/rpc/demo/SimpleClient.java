package com.lingfeng.rpc.demo;

import cn.hutool.core.util.RandomUtil;
import com.lingfeng.rpc.client.nettyclient.BizNettyClient;
import com.lingfeng.rpc.client.nettyclient.NettyClientFactory;
import com.lingfeng.rpc.model.Address;
import com.lingfeng.rpc.server.nettyserver.BizNettyServer;
import com.lingfeng.rpc.server.nettyserver.NettyServerFactory;
import com.lingfeng.rpc.util.SystemClock;
import com.lingfeng.rpc.util.TimeUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.util.CharsetUtil;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * @Author: wz
 * @Date: 2022/5/16 18:02
 * @Description:
 */
public class SimpleClient {


    protected static ByteBuf buildMsg(String msg) {
//        CompositeByteBuf byteBufs = Unpooled.compositeBuffer();
        return Unpooled.copiedBuffer(msg, CharsetUtil.UTF_8);
    }

    public static void main(String[] args) throws InterruptedException {
        new Thread(() -> {
            BizNettyClient client = NettyClientFactory.buildSimpleClient(new Address("127.0.0.1", 9999),
                    () -> Arrays.asList(new DemoClientHandler()));
            client.start();
        }).start();

        while (true) {
            TimeUnit.MILLISECONDS.sleep(2000);
        }
    }
}
