package com.lingfeng.rpc.server.nettyserver;

import com.lingfeng.rpc.coder.Coder;
import com.lingfeng.rpc.coder.CoderFactory;
import com.lingfeng.rpc.coder.safe.SafeCoder;
import com.lingfeng.rpc.model.Address;
import com.lingfeng.rpc.server.listener.ServerReconnectFutureListener;
import com.lingfeng.rpc.server.handler.BizServerHandler;
import com.lingfeng.rpc.server.handler.ServerHeartHandler;
import com.lingfeng.rpc.server.handler.ServerIdleHandler;

/**
 * @Author: wz
 * @Date: 2022/5/12 11:20
 * @Description:
 */
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
        server
                //空闲处理器
               // .addHandler(ServerIdleHandler.getIdleHandler(), ServerIdleHandler.NAME)
                //心跳处理器
               // .addHandler(new ServerHeartHandler(), ServerHeartHandler.NAME)
                //业务处理器
                .addHandler(new BizServerHandler(), null)
                //监听器
                .addListener(new ServerReconnectFutureListener());
        return server;
    }
}
