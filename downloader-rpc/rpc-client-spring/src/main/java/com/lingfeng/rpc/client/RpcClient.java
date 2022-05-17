package com.lingfeng.rpc.client;

import cn.hutool.core.util.RandomUtil;
import com.lingfeng.rpc.client.core.NettyReqHandler;
import com.lingfeng.rpc.client.nettyclient.BizNettyClient;
import com.lingfeng.rpc.client.nettyclient.NettyClientFactory;
import com.lingfeng.rpc.constant.Cmd;
import com.lingfeng.rpc.data.Frame;
import com.lingfeng.rpc.model.Address;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RpcClient {


    @PostConstruct
    public void init() {

        BizNettyClient client = NettyClientFactory.buildBizNettyClient(new Address("127.0.0.1", 9999),
                () -> Arrays.asList(new NettyReqHandler()));
        client.start();

        AtomicInteger data = new AtomicInteger(RandomUtil.randomInt(90, 1000));
        new Thread(() -> {
            while (true) {
                try {
                    TimeUnit.SECONDS.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                int i = data.incrementAndGet();
                Frame<String> frame = new Frame<>();
                frame.setData("RPC Client data:" + i);
                client.writeAndFlush(frame, Cmd.REQUEST);
            }
        });
        //.start();
    }
}
