package com.lingfeng.rpc;

import com.lingfeng.rpc.client.handler.HeartHandler;
import com.lingfeng.rpc.client.handler.IdleHandler;
import com.lingfeng.rpc.client.handler.ReConnectFutureListener;
import com.lingfeng.rpc.client.handler.MyClientHandler;
import com.lingfeng.rpc.client.nettyclient.NettyClient;
import com.lingfeng.rpc.coder.Coder;
import com.lingfeng.rpc.coder.CoderFactory;
import com.lingfeng.rpc.coder.safe.SafeCoder;
import com.lingfeng.rpc.constant.Cmd;
import com.lingfeng.rpc.model.Address;
import com.lingfeng.rpc.model.TempData;
import com.lingfeng.rpc.server.handler.MyServerHandler;
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


        NettyClient client1 = new NettyClient();
        client1.setAddress(new Address("127.0.0.1", 9999));
        CoderFactory coderFactory = CoderFactory.getInstance();
        Coder generate = coderFactory.generate(SafeCoder.class);
        client1
                //自定义协议
                .addHandler(generate.type(), null)
                .addHandler(generate.decode(), null)
                .addHandler(generate.encode(), null)
                //空闲处理器
                .addHandler(IdleHandler.getIdleHandler(), IdleHandler.NAME)
                //心跳处理器
                .addHandler(new HeartHandler(), HeartHandler.NAME)
                //业务处理器
                .addHandler(new MyClientHandler(), null)
                //监听器
                .addListener(new ReConnectFutureListener());
        //启动
        client1.start();


        TimeUnit.SECONDS.sleep(8);
        int x = 100;
        int finalX = x;

        client1.writeAndFlush("ddd", Cmd.HEARTBEAT);

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
                    TempData tempData = new TempData();
                    tempData.setId(i);
                    tempData.setName(data);
                    client1.writeAndFlush(tempData, Cmd.REQUEST);
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


        new Thread(() -> {
            for (int i = 0; i < x; i++) {
                try {
                    // TimeUnit.MILLISECONDS.sleep(5000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                String data = null;
                try {
                    data = "[client1] bbq-" + i;
                    // client2.getHandler().writeAndFlush(data, FrameType.HEARTBEAT);
                } catch (Exception e) {
                    log.info("发送{} 失败", data);
                    // log.error(e.getMessage(), e);
                    try {
                        TimeUnit.MILLISECONDS.sleep(2000);
                    } catch (InterruptedException interruptedException) {
                        interruptedException.printStackTrace();
                    }
                    i--;
                }
            }
        });
        //.start();


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
