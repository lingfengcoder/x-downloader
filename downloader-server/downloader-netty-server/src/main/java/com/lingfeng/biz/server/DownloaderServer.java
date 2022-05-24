package com.lingfeng.biz.server;

import com.lingfeng.biz.server.constant.MsgType;
import com.lingfeng.biz.server.handler.BizBasicServerHandler;
import com.lingfeng.biz.server.handler.BizMessageServerHandler;
import com.lingfeng.biz.server.model.NodeClient;
import com.lingfeng.rpc.constant.Cmd;
import com.lingfeng.rpc.data.Frame;
import com.lingfeng.rpc.model.Address;
import com.lingfeng.rpc.server.nettyserver.BizNettyServer;
import com.lingfeng.rpc.server.nettyserver.NettyServerFactory;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;


/**
 * @Author: wz
 * @Date: 2022/5/18 14:59
 * @Description:
 */
@Slf4j
@Component
public class DownloaderServer {

    @Autowired
    private BizBasicServerHandler bizBasicServerHandler;
    @Autowired
    private BizMessageServerHandler bizMessageServerHandler;

    private static volatile BizNettyServer nettyServer;

    public static BizNettyServer getInstance() {
        return nettyServer;
    }

    @PostConstruct
    private void startServer() {
        nettyServer = NettyServerFactory.buildBizNettyServer(new Address("127.0.0.1", 9999),
                bizBasicServerHandler, bizMessageServerHandler);
        nettyServer.start();
        log.info("netty服务已启动");
    }

    public BizNettyServer getNettyServer() {
        return nettyServer;
    }
}
