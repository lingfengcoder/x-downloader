package com.lingfeng.rpc;

import java.util.concurrent.TimeUnit;

/**
 * @Author: wz
 * @Date: 2022/5/7 18:35
 * @Description:
 */
public class Test {
    public static void main(String[] args) throws InterruptedException {
        NettyServer server = NettyServer.getInstance();
        server.start("127.0.0.1", 9999);


        NettyClient client = NettyClient.getInstance();
        client.start("127.0.0.1", 9999);


        int x = 10;
        for (int i = 0; i < x; i++) {
            TimeUnit.MILLISECONDS.sleep(100);
            MyClientHandler.getInstance().write("bbq-" + i);
        }
        while (true) {
            TimeUnit.SECONDS.sleep(1);
        }
    }
}
