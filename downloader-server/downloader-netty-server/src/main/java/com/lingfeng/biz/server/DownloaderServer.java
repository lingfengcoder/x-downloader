package com.lingfeng.biz.server;

import com.lingfeng.biz.downloader.model.BasicCmd;
import com.lingfeng.biz.downloader.model.BasicFrame;
import com.lingfeng.biz.downloader.model.DownloadTask;
import com.lingfeng.biz.server.clientapi.ClientDemopi;
import com.lingfeng.biz.server.handler.messagehandler.biz.BizBasicServerHandler;
import com.lingfeng.biz.server.handler.messagehandler.biz.BizMessageServerHandler;
import com.lingfeng.rpc.ann.EnableRpcClient;
import com.lingfeng.rpc.handler.SpringServerProxyInvokeHandler;
import com.lingfeng.rpc.invoke.RemoteInvoke;
import com.lingfeng.rpc.model.Address;
import com.lingfeng.rpc.server.nettyserver.BizNettyServer;
import com.lingfeng.rpc.server.nettyserver.NettyServerFactory;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;


/**
 * @Author: wz
 * @Date: 2022/5/18 14:59
 * @Description:
 */
@Slf4j
@Order(1)
@Component
@EnableRpcClient("com.lingfeng.biz.server.clientapi")
public class DownloaderServer implements CommandLineRunner {

    @Autowired
    private BizBasicServerHandler bizBasicServerHandler;
    @Autowired
    private BizMessageServerHandler bizMessageServerHandler;
    @Autowired
    @Qualifier("dispatcherSenderThreadPool")
    private ThreadPoolTaskExecutor handleRpcThreadPool;
    private static volatile BizNettyServer nettyServer;

    public static BizNettyServer getInstance() {
        return nettyServer;
    }

    @PreDestroy
    public void destroy() {
        if (nettyServer != null) {
            nettyServer.stop();
        }
    }


    private void startServer() {
        //rpc 处理器
        SpringServerProxyInvokeHandler serverRpcProxyInvokeHandler = new SpringServerProxyInvokeHandler(handleRpcThreadPool);
        nettyServer = NettyServerFactory.buildBizNettyServer(new Address("127.0.0.1", 9999),
                bizBasicServerHandler, bizMessageServerHandler, serverRpcProxyInvokeHandler);
        nettyServer.start();
        log.info("netty服务已启动");


        Thread dcded = new Thread(() -> {
            int x = 0;
            while (true) {
                try {
                    TimeUnit.MILLISECONDS.sleep(5000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                Collection<Channel> channels = nettyServer.allChannels();
                for (Channel channel : channels) {
                    RemoteInvoke instance = RemoteInvoke.getInstance(nettyServer, channel);
                    ClientDemopi clientDemopi = instance.getBean(ClientDemopi.class);
                    // ClientApi clientApi = instance.getBean(ClientApi.class);
                    DownloadTask task = new DownloadTask().setId(x).setCreateTime(new Date()).setNode(channel.id().asLongText());
                    //clientApi.listenTask(task);
                    log.info("开始发送 {}号 任务", x++);
                    BasicFrame<Object> frame = BasicFrame.builder().data("任务->" + x).clientId(channel.id().asLongText()).cmd(BasicCmd.REG).build();
                    frame.setData("任务" + x);
                    frame.setTarget("%%clientTest%%");
                    frame.setCreateTime(new Date());
                    frame.setRedoCount(1);
                    frame.setUrl("dcded");
                    //clientDemopi.clientTest(frame);
                    clientDemopi.testDownloadTask(task);
//                    DownloadTask task = DownloadTask.builder().id(x).createTime(new Date()).node(channel.id().asLongText()).url("http://baidu.com").build();
                }
            }
        });
        //   dcded.start();
    }

    public BizNettyServer getNettyServer() {
        return nettyServer;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("springboot启动完毕，开始启动rpcNetty");
        startServer();
    }
}
