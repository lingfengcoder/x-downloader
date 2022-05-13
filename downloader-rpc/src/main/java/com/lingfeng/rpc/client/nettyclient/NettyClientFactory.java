package com.lingfeng.rpc.client.nettyclient;

import com.lingfeng.rpc.client.handler.HeartHandler;
import com.lingfeng.rpc.client.handler.IdleHandler;
import com.lingfeng.rpc.client.handler.MyClientHandler;
import com.lingfeng.rpc.client.handler.ReConnectFutureListener;
import com.lingfeng.rpc.coder.Coder;
import com.lingfeng.rpc.coder.CoderFactory;
import com.lingfeng.rpc.coder.safe.SafeCoder;
import com.lingfeng.rpc.model.Address;

/**
 * @Author: wz
 * @Date: 2022/5/12 11:20
 * @Description:
 */
public class NettyClientFactory {

    public static <T> T generateClient(Address address, Class<T> clazz) {
        if (clazz.equals(BizNettyClient.class)) {
            return (T) buildBizNettyClient(address);
        }
        return null;
    }

    private static BizNettyClient buildBizNettyClient(Address address) {
        BizNettyClient client = new BizNettyClient();
        client.setAddress(address);
        //通过配置的方式，可以保证每次重启都获取新的handler对象 ,从而避免了@Sharable
        client.config(_client -> {
            //coder 不允许共享需要单独添加
            //自定义传输协议
            CoderFactory coderFactory = CoderFactory.getInstance();
            Coder generate = coderFactory.generate(SafeCoder.class);
            _client
                    .addHandler(generate.type())
                    .addHandler(generate.decode())
                    .addHandler(generate.encode())
                    //空闲处理器
                    .addHandler(IdleHandler.getIdleHandler())
                    //心跳处理器
                    .addHandler(new HeartHandler())// HeartHandler.NAME
                    //业务处理器
                    .addHandler(new MyClientHandler())
                    //监听器
                    .addListener(new ReConnectFutureListener());
        });
        return client;
    }
}
