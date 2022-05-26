package com.lingfeng.biz.downloader;


import cn.hutool.core.lang.UUID;
import cn.hutool.core.net.NetUtil;
import com.lingfeng.biz.downloader.config.NodeConfig;
import com.lingfeng.biz.downloader.netty.NettyReqHandler;
import com.lingfeng.rpc.client.nettyclient.BizNettyClient;
import com.lingfeng.rpc.client.nettyclient.NettyClientFactory;
import com.lingfeng.rpc.model.Address;
import com.lingfeng.rpc.util.StringUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

/**
 * @Author: wz
 * @Date: 2022/5/26 13:41
 * @Description:
 */
@Slf4j
public class NettyRpcClient {
    private static volatile ClientConfig clientConfig;
    private static final NettyRpcClient instance = new NettyRpcClient();

    public String getClientId() {
        return clientConfig.getClientId();
    }

    public static NettyRpcClient getInstance() {
        return instance;
    }


    public static void startClient() throws Exception {
        //启动ioc环境
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(NodeConfig.class);

//        context.setEnvironment();
//        Resource resource = new ClassPathResource("client.yml");
//        context.setResourceLoader(resource);

        clientConfig = context.getBean(ClientConfig.class);
        clientConfig.loadClientConfig();
        Address address = clientConfig.getAddress();

        BizNettyClient client = NettyClientFactory.buildBizNettyClient(address,
                () -> Arrays.asList(new NettyReqHandler()));
        client.start();
    }


    public static void main(String[] args) throws Exception {
        startClient();
    }
}
