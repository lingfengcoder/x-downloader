package com.lingfeng.biz.server;

import com.lingfeng.biz.server.constant.MsgType;
import com.lingfeng.biz.server.handler.BizMessageServerHandler;
import com.lingfeng.biz.server.model.DownloaderClient;
import com.lingfeng.rpc.constant.Cmd;
import com.lingfeng.rpc.data.Frame;
import com.lingfeng.rpc.model.Address;
import com.lingfeng.rpc.server.nettyserver.BizNettyServer;
import com.lingfeng.rpc.server.nettyserver.NettyServerFactory;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;

/**
 * @Author: wz
 * @Date: 2022/5/18 14:59
 * @Description:
 */
@Slf4j
@Component
public class DownloaderServer {

    private volatile BizNettyServer nettyServer;

    @PostConstruct
    private void startServer() {
        nettyServer = NettyServerFactory.buildBizNettyServer(
                new Address("127.0.0.1", 9999),
                () -> Arrays.asList(new BizMessageServerHandler()));
        nettyServer.start();
        log.info("netty服务已启动");
    }

    public <M> void sendMsg(DownloaderClient client, M msg, MsgType msgType) {
        Channel channel = client.getChannel();
        Frame<M> fame = new Frame<>();
        fame.setTarget(msgType.name());
        fame.setData(msg);
        nettyServer.writeAndFlush(channel, fame, Cmd.REQUEST);
    }

    public BizNettyServer getNettyServer() {
        return nettyServer;
    }
}
