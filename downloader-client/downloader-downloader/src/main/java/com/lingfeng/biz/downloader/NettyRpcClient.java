package com.lingfeng.biz.downloader;


import com.lingfeng.biz.downloader.config.NodeConfig;
import com.lingfeng.biz.downloader.model.BasicCmd;
import com.lingfeng.biz.downloader.model.BasicFrame;
import com.lingfeng.biz.downloader.netty.BasicHandler;
import com.lingfeng.biz.downloader.netty.NettyReqHandler;
import com.lingfeng.biz.downloader.netty.serverapi.RegisterAction;
import com.lingfeng.rpc.client.handler.AbsClientHandler;
import com.lingfeng.rpc.client.nettyclient.BizNettyClient;
import com.lingfeng.rpc.client.nettyclient.NettyClientFactory;
import com.lingfeng.rpc.constant.Cmd;
import com.lingfeng.rpc.data.RpcInvokeFrame;
import com.lingfeng.rpc.frame.SafeFrame;
import com.lingfeng.rpc.handler.SpringInvokeHandler;
import com.lingfeng.rpc.invoke.ProxySender;
import com.lingfeng.rpc.invoke.RemoteInvoke;
import com.lingfeng.rpc.model.Address;
import com.lingfeng.rpc.proxy.DemoFunction;
import com.lingfeng.rpc.proxy.JdkDynamicProxyUtil;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
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
        //启动客户端
        Address address = new Address(config.getServerHost(), config.getServerPort());
        BizNettyClient bizNettyClient = NettyClientFactory.buildBizNettyClient(address, () -> Arrays.asList(new SpringInvokeHandler(), new BasicHandler(), new NettyReqHandler()));
        defiedClient(config, bizNettyClient);
        bizNettyClient.start();

        new Thread(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            test();
        }).start();

    }

    private static void prepared() {
        instance.loadApplicationContext();
    }

    private static void defiedClient(NodeConfig config, BizNettyClient bizNettyClient) {
        instance.config = config;
        instance.nettyClient = bizNettyClient;
    }


    private static void test() {
        NettyRpcClient client = NettyRpcClient.getInstance();
        String clientId = client.getClientId();


        //注册帧
        BasicFrame<Object> frame = BasicFrame.builder()
                .cmd(BasicCmd.REG)
                .clientId(clientId)
                .data("66778899")
                .build();

        RemoteInvoke instance = RemoteInvoke.getInstance();
//        //像调用本地方法一样，调用远程服务方法
        RegisterAction registerAction = instance.getDynamicProxy2(RegisterAction.class);
//        log.info("testproxy={}", registerAction);
        registerAction.register(frame);


//        RegisterAction newProxyInstance = JdkDynamicProxyUtil.proxyInvoke(RegisterAction.class, (proxy, method, args1) -> {
//            if (method.getName().equals("toString")) {
//                return "RegisterAction no toString ";
//            }
//            log.info("current do it");
//            log.info("current do it");
//            log.info("current do it");
//            log.info("current do it proxy={} method name={} args={}", proxy, method.getName(), args1);
//            RpcInvokeFrame req = new RpcInvokeFrame();
//            //目标方法名称
//            req.setMethodName("register");
//            //参数
//            req.setArguments(args1);
//            nettyClient.writeAndFlush(req, Cmd.REQUEST);
//            //method.invoke(proxy, args1);
//            return null;
//        });
//        newProxyInstance.register(frame);


        //远程服务调用
        // ProxySender proxySender = RemoteInvoke.getInstance().getSender();
        //proxySender.writeAndFlush(req);

        int x = 5;
        for (int i = 0; i < x; i++) {
            // nettyClient.writeAndFlush(req, Cmd.REQUEST);
            // proxy.register(frame);
        }
    }
}
