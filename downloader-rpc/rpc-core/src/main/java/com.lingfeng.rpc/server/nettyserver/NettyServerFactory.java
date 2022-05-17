package com.lingfeng.rpc.server.nettyserver;

import com.lingfeng.rpc.client.handler.*;
import com.lingfeng.rpc.client.nettyclient.BizNettyClient;
import com.lingfeng.rpc.coder.Coder;
import com.lingfeng.rpc.coder.CoderFactory;
import com.lingfeng.rpc.coder.safe.SafeCoder;
import com.lingfeng.rpc.model.Address;
import com.lingfeng.rpc.server.handler.AbsServerHandler;
import com.lingfeng.rpc.server.handler.ServerHeartHandler;
import com.lingfeng.rpc.server.handler.ServerIdleHandler;
import com.lingfeng.rpc.server.listener.ServerReconnectFutureListener;
import com.lingfeng.rpc.server.handler.BizServerHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;


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
        return buildBizNettyServer(address, () -> Collections.singletonList((new BizServerHandler())));
    }

    public static <T> BizNettyServer buildBizNettyServer(Address address, Supplier<List<AbsServerHandler<T>>> func) {
        BizNettyServer server = new BizNettyServer();
        server.setAddress(address);
        server.config(_server -> {
            log.info("====== _server accept ===== ");
            CoderFactory coderFactory = CoderFactory.getInstance();
            Coder generate = coderFactory.generate(SafeCoder.class);
            _server
                    .addHandler(generate.type())
                    .addHandler(generate.decode())
                    .addHandler(generate.encode())
                    //.addHandler(ServerIdleHandler.getIdleHandler())
                    //心跳处理器
                    .addHandler(new ServerHeartHandler())
                    //监听器
                    .addListener(new ServerReconnectFutureListener());
            //添加业务处理器
            if (func != null) {
                List<AbsServerHandler<T>> handlers = func.get();
                for (AbsServerHandler<T> handler : handlers) {
                    _server.addHandler(handler);
                }
            }
        });
        return server;
    }

    public static <T> BizNettyServer buildSimpleNettyServer(Address address, Supplier<List<AbsServerHandler<T>>> func) {
        BizNettyServer server = new BizNettyServer();
        server.setAddress(address);
        server.config(_server -> {
            log.info("====== SimpleNettyServer accept ===== ");

//            CoderFactory coderFactory = CoderFactory.getInstance();
//            Coder generate = coderFactory.generate(SafeCoder.class);
//            _server
//                    .addHandler(generate.type())
//                    .addHandler(generate.decode())
//                    .addHandler(generate.encode());

          //  _server.addHandler(ServerIdleHandler.getIdleHandler());
            //心跳处理器
            //_server.addHandler(new ServerHeartHandler());
            //监听器
            _server.addListener(new ServerReconnectFutureListener());
            //添加业务处理器
            if (func != null) {
                List<AbsServerHandler<T>> handlers = func.get();
                for (AbsServerHandler<T> handler : handlers) {
                    _server.addHandler(handler);
                }
            }
        });
        return server;
    }
}
