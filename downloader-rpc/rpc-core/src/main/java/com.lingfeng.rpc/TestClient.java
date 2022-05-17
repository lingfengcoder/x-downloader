package com.lingfeng.rpc;

import com.lingfeng.rpc.client.nettyclient.BizNettyClient;
import com.lingfeng.rpc.client.nettyclient.NettyClientFactory;
import com.lingfeng.rpc.coder.safe.DataFrame;
import com.lingfeng.rpc.constant.Cmd;
import com.lingfeng.rpc.model.Address;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author: wz
 * @Date: 2022/5/7 18:35
 * @Description:
 */
@Slf4j
public class TestClient {
    public static void main(String[] args) throws InterruptedException {


        BizNettyClient client1 = NettyClientFactory.generateClient(new Address("127.0.0.1", 9999),
                BizNettyClient.class);
        client1.start();

        // TimeUnit.SECONDS.sleep(8);
        log.info("主动关闭客户端");
        //client1.close();

        int x = 30;
        int finalX = x;
        AtomicInteger left = new AtomicInteger(x);
//        client1.writeAndFlush("ddd", Cmd.HEARTBEAT);

        new Thread(() -> {
            for (int i = 0; i < Integer.MAX_VALUE; i++) {
                try {
                    TimeUnit.MILLISECONDS.sleep(2000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                String data = null;
                int tmp = left.decrementAndGet();
                try {
                    if (tmp == 0) {
                        log.info("全部数据发送完毕");
                        break;
                    }
                    data = "[client1] bbq-" + tmp;
                    DataFrame<String> dataFrame = new DataFrame<>();
                    dataFrame.setData(data);
                    // client1.writeAndFlush(dataFrame, Cmd.REQUEST);
                } catch (Exception e) {
                    log.info("发送{} 失败", data);
                    //  log.error(e.getMessage(), e);
                    try {
                        TimeUnit.MILLISECONDS.sleep(2000);
                    } catch (InterruptedException interruptedException) {
                        interruptedException.printStackTrace();
                    }
                    left.set(++tmp);
                }
            }
        });
        //.start();


        //TimeUnit.SECONDS.sleep(5);
        // log.info("开始关闭 channel 1");
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
            log.info("get tmp={}", left.get());
            TimeUnit.SECONDS.sleep(3);
        }
    }
}
