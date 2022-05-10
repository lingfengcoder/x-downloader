package com.lingfeng.rpc;

import com.lingfeng.rpc.client.MyChannelFutureListener;
import com.lingfeng.rpc.client.MyClientHandler;
import com.lingfeng.rpc.client.NettyClient;
import com.lingfeng.rpc.model.Address;
import com.lingfeng.rpc.server.MyServerHandler;
import com.lingfeng.rpc.server.NettyServer;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * @Author: wz
 * @Date: 2022/5/7 18:35
 * @Description:
 */
@Slf4j
public class Test {
    public static void main(String[] args) throws InterruptedException {
        NettyServer server = NettyServer.getInstance();
        MyServerHandler serverHandler = new MyServerHandler();
        server.setAddress(new Address("127.0.0.1", 9999))
                .setServerHandler(serverHandler).start();


        NettyClient client = NettyClient.getInstance();
        MyClientHandler handler1 = new MyClientHandler();
        client.setAddress(new Address("127.0.0.1", 9999))
                .setHandler(handler1).setListener(new MyChannelFutureListener()).start();


//        NettyClient client2 = new NettyClient();
//        MyClientHandler handler2 = new MyClientHandler();
//        client2.setAddress(new Address("127.0.0.1", 9999))
//                .setHandler(handler2).setListener(new MyChannelFutureListener()).start();

        TimeUnit.SECONDS.sleep(5);
        int x = 1000;
        int finalX = x;
        new Thread(() -> {
            for (int i = 0; i < finalX; i++) {
                try {
                    // TimeUnit.MILLISECONDS.sleep(100);

                } catch (Exception e) {
                    e.printStackTrace();
                }
                String data = null;
                try {
                    data = "[client1] bbq-" + i;
                    handler1.write(data);
                } catch (Exception e) {
                    log.info("发送{} 失败", data);
                    //  log.error(e.getMessage(), e);
                    try {
                        TimeUnit.MILLISECONDS.sleep(2000);
                    } catch (InterruptedException interruptedException) {
                        interruptedException.printStackTrace();
                    }
                    i--;
                }
            }
        }).start();


//        new Thread(() -> {
//            for (int i = 0; i < x; i++) {
//                try {
//                    // TimeUnit.MILLISECONDS.sleep(5000);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//                String data = null;
//                try {
//                    data = "[client1] bbq-" + i;
//                    handler2.write(data);
//                } catch (Exception e) {
//                    log.info("发送{} 失败", data);
//                    // log.error(e.getMessage(), e);
//                    try {
//                        TimeUnit.MILLISECONDS.sleep(2000);
//                    } catch (InterruptedException interruptedException) {
//                        interruptedException.printStackTrace();
//                    }
//                    i--;
//                }
//            }
//        }).start();


        //TimeUnit.SECONDS.sleep(5);
        log.info("开始关闭 channel 1");
        // serverHandler.closeChannel(1);

        TimeUnit.SECONDS.sleep(5);
        // log.info("开始关闭server");
        // server.stop();

        TimeUnit.SECONDS.sleep(5);
        // log.info("开始重启server");
        // server.restart();

        TimeUnit.SECONDS.sleep(5);
        //client2.close();


        while (true) {
            TimeUnit.SECONDS.sleep(1);
        }
    }
}
