package com.lingfeng.rpc;


import com.lingfeng.rpc.model.Address;
import com.lingfeng.rpc.server.nettyserver.BizNettyServer;
import com.lingfeng.rpc.server.nettyserver.NettyServerFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * @Author: wz
 * @Date: 2022/5/7 18:35
 * @Description:
 */
@Slf4j
public class TestServer {
    public static void main(String[] args) throws InterruptedException {
        BizNettyServer server = NettyServerFactory.generateServer(new Address("127.0.0.1", 9999), BizNettyServer.class);
        server.start();
        while (server.state() != 1) {
        }

        while (server.state() == 1) {
            TimeUnit.SECONDS.sleep(2);
        }
    }
}
