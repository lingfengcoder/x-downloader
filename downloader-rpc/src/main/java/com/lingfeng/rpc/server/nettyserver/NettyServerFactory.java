package com.lingfeng.rpc.server.nettyserver;

import com.lingfeng.rpc.coder.Coder;
import com.lingfeng.rpc.coder.CoderFactory;
import com.lingfeng.rpc.coder.safe.SafeCoder;
import com.lingfeng.rpc.model.Address;
import com.lingfeng.rpc.server.handler.ServerHeartHandler;
import com.lingfeng.rpc.server.listener.ServerReconnectFutureListener;
import com.lingfeng.rpc.server.handler.BizServerHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;


/**
 * @Author: wz
 * @Date: 2022/5/12 11:20
 * @Description:
 */
@Slf4j
public class NettyServerFactory {

    public static <T> T generateServer(Address address, Class<T> clazz) {
        if (clazz.equals(BizNettyServer.class)) {
            return (T) buildBizNettyServer(address);
        }
        return null;
    }

    private static BizNettyServer buildBizNettyServer(Address address) {
        BizNettyServer server = new BizNettyServer();
        server.setAddress(address);
        server.config(new Consumer<AbsNettyServer>() {
            @Override
            public void accept(AbsNettyServer _server) {
                log.info("=================  accept ===== ");
                CoderFactory coderFactory = CoderFactory.getInstance();
                Coder generate = coderFactory.generate(SafeCoder.class);
                _server
                        .addHandler(generate.type())
                        .addHandler(generate.decode())
                        .addHandler(generate.encode())
                        //.addHandler(ServerIdleHandler.getIdleHandler())
                        //心跳处理器
                        .addHandler(new ServerHeartHandler())
                        //业务处理器
                        .addHandler(new BizServerHandler())
                        //监听器
                        .addListener(new ServerReconnectFutureListener());
            }
        });
//        server.config(_server -> {//空闲处理器
//            //coder 不允许共享需要单独添加
//            //自定义传输协议
//
//        });
        return server;
    }
}
