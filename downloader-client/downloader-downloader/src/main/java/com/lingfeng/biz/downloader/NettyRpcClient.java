package com.lingfeng.biz.downloader;


import cn.hutool.extra.spring.SpringUtil;
import com.lingfeng.biz.downloader.config.NodeConfig;
import com.lingfeng.biz.downloader.netty.BasicHandler;
import com.lingfeng.biz.downloader.netty.NettyReqHandler;
import com.lingfeng.biz.downloader.netty.serverapi.RegisterAction;
import com.lingfeng.rpc.client.nettyclient.BizNettyClient;
import com.lingfeng.rpc.client.nettyclient.NettyClientFactory;
import com.lingfeng.rpc.handler.SpringClientProxyInvokeHandler;
import com.lingfeng.rpc.model.Address;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @Author: wz
 * @Date: 2022/5/26 13:41
 * @Description:
 */
@Slf4j
@Getter
public class NettyRpcClient {

    private BizNettyClient nettyClient;
    private volatile NodeConfig config;
    private AnnotationConfigApplicationContext context;
    private static final NettyRpcClient instance = new NettyRpcClient();

    public static NettyRpcClient getInstance() {
        return instance;
    }

    public String getClientId() {
        return config.getNode();
    }

    public static void main(String[] args) throws Exception {
        start();
    }

    //启动ioc环境
    private void loadApplicationContext() {
        context = new AnnotationConfigApplicationContext(NodeConfig.class);
        instance.config = context.getBean(NodeConfig.class);
    }

    private static void start() {
        prepared();
        NodeConfig config = instance.config;
        ThreadPoolTaskExecutor handlerThreadPool = SpringUtil.getBean("downloaderThreadPool");
        //启动客户端
        Address address = new Address(config.getServerHost(), config.getServerPort());
        BizNettyClient bizNettyClient = NettyClientFactory.buildBizNettyClient(address, () -> Arrays.asList(new SpringClientProxyInvokeHandler(handlerThreadPool), new BasicHandler(), new NettyReqHandler()));
        defiedClient(config, bizNettyClient);
        bizNettyClient.start();

        new Thread(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            log.info("开始测试复杂参数调用");
            RegisterAction registerAction = SpringUtil.getBean(RegisterAction.class);
            ConcurrentHashMap<String, Integer> data = new ConcurrentHashMap<>();
            data.put("1", 11);
            data.put("2", 22);
            data.put("13", 1313);
            data.put("4", 44);
            registerAction.test(data);
        }).start();
    }

    private static void prepared() {
        instance.loadApplicationContext();
    }

    private static void defiedClient(NodeConfig config, BizNettyClient bizNettyClient) {
        instance.config = config;
        instance.nettyClient = bizNettyClient;
    }


}
