package com.lingfeng.rpc.demo;

import cn.hutool.core.util.RandomUtil;
import com.lingfeng.rpc.client.nettyclient.BizNettyClient;
import com.lingfeng.rpc.client.nettyclient.NettyClientFactory;
import com.lingfeng.rpc.coder.Coder;
import com.lingfeng.rpc.coder.CoderFactory;
import com.lingfeng.rpc.coder.safe.SafeCoder;
import com.lingfeng.rpc.constant.Cmd;
import com.lingfeng.rpc.frame.SafeFrame;
import com.lingfeng.rpc.model.Address;
import com.lingfeng.rpc.server.nettyserver.BizNettyServer;
import com.lingfeng.rpc.server.nettyserver.NettyServerFactory;
import com.lingfeng.rpc.trans.MessageTrans;
import com.lingfeng.rpc.util.GsonTool;
import com.lingfeng.rpc.util.SystemClock;
import com.lingfeng.rpc.util.TimeUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.util.CharsetUtil;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Author: wz
 * @Date: 2022/5/16 18:02
 * @Description:
 */
public class SimpleServer {


    protected static ByteBuf buildMsg(String msg) {
//        CompositeByteBuf byteBufs = Unpooled.compositeBuffer();
        return Unpooled.copiedBuffer(msg, CharsetUtil.UTF_8);
    }

    public static void main(String[] args) throws InterruptedException {

        new Thread(() -> {
            BizNettyServer server = NettyServerFactory.buildSimpleNettyServer(new Address("127.0.0.1", 9999),
                    () -> Arrays.asList(new DemoServerHandler()));
            server.start();
            while (true) {
                try {
                   // TimeUnit.MILLISECONDS.sleep(1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Collection<Channel> channels = server.allChannels();
                for (Channel channel : channels) {
                    String data = RandomUtil.randomString(8) + " " + TimeUtil.formatDate(SystemClock.now());
                    // channel.writeAndFlush(data);
                    //SafeFrame<String> retData = MessageTrans.dataFrame(data, Cmd.REQUEST, RandomUtil.randomLong(0, 9));
                    channel.writeAndFlush(buildMsg(data));
                    // server.writeAndFlush(channel, data, Cmd.REQUEST);
                    //server.writeAndFlush(channel, RandomUtil.randomDouble(100));
                }
            }
        }).start();

        while (true) {
            TimeUnit.MILLISECONDS.sleep(2000);
        }
    }
}
