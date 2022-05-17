package com.lingfeng.rpc;


import com.lingfeng.rpc.coder.safe.DataFrame;
import com.lingfeng.rpc.constant.Cmd;
import com.lingfeng.rpc.model.Address;
import com.lingfeng.rpc.server.nettyserver.BizNettyServer;
import com.lingfeng.rpc.server.nettyserver.NettyServerFactory;
import com.lingfeng.rpc.util.SystemClock;
import com.lingfeng.rpc.util.TimeUtil;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * @Author: wz
 * @Date: 2022/5/7 18:35
 * @Description:
 */
@Slf4j
public class TestServer {
    public static void main(String[] args) throws InterruptedException {
        BizNettyServer server = NettyServerFactory.generateServer(new Address("127.0.0.1", 9999),
                BizNettyServer.class);
        server.start();
        while (server.state() != 1) {
        }

        while (server.state() == 1) {

            // server.showChannels();
            Collection<Channel> channels = server.allChannels();
            for (Channel channel : channels) {
                DataFrame<String> frame = new DataFrame<>();
                frame.setData("这是来自服务器的数据:" + TimeUtil.formatDate(SystemClock.now()));
                server.writeAndFlush(channel, frame, Cmd.REQUEST);
            }
            TimeUnit.MILLISECONDS.sleep(200);
        }
    }
}
