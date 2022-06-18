package com.lingfeng.biz.downloader.config;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.net.NetUtil;
import com.lingfeng.rpc.ann.EnableRpcClient;
import com.lingfeng.rpc.util.StringUtils;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
//import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import javax.annotation.PostConstruct;
import java.io.BufferedOutputStream;
import java.util.Properties;

/**
 * @Author: wz
 * @Date: 2021/11/9 11:53
 * @Description:
 */
@Getter
@Setter
@Configuration
@Order(1)
//@Accessors(fluent = true)
//@NacosPropertySource(dataId = "biz-downloader-dev.yaml", groupId = "biz", autoRefreshed = true)
//@RefreshScope//动态刷新bean
@EnableRpcClient("com.lingfeng.biz.downloader.netty.serverapi")
@PropertySource("classpath:" + NodeConfig.PROPERTY_FILE)
@ComponentScans({@ComponentScan("com.lingfeng.biz.downloader"), @ComponentScan("com.lingfeng.rpc"), @ComponentScan("cn.hutool")})
public class NodeConfig {
    public final static String PROPERTY_FILE = "client.properties";
    //单节点消费开关
    @Value("${client.enable}")
    private boolean enable;
    //节点名称
    @Value("${client.node}")
    private String node;
    //服务端地址
    @Value("${client.server.host}")
    private String serverHost;
    @Value("${client.server.port}")
    private int serverPort;
    //内部定时任务是否开启
    @Value("${client.enableSchedule}")
    private boolean enableSchedule;
    //单节点允许同时下载(影响下载执行并发数)
    @Value("${client.channelLimit}")
    private Integer channelLimit;
    //单节点允许同时下载(影响下载执行并发数)
    @Value("${client.concurrentLimit}")
    private Integer concurrentLimit;
    //单节点队列最大 正常消息数(影响下载执行队列长度)
    @Value("${client.queueLenLimit}")
    private Integer queueLenLimit;
    //队列发送限制数
    @Value("${client.sendLimit}")
    private Integer sendLimit;
    //单节点队列最大 重试消息数
    @Value("${client.retryLenLimit}")
    private Integer retryLenLimit;
    //失败的任务重试的次数
    @Value("${client.failedRetryCount}")
    private Integer failedRetryCount;
    //下载完成后是否上传FTP
    @Value("${client.autoUploadFtp}")
    private Boolean autoUploadFtp;
    //临时下载文件的存活时间
    @Value("${client.tmpIndex.liveTime}")
    private Long tmpIndexLiveTime;

    //临时下载文件的存活时间
    // @Value("${client.tmpIndex.timeCron}")
    // private String tmpIndexTimeCron;


    public NodeConfig() {
        System.out.println(this);
    }

    @PostConstruct
    public void init() throws Exception {
        if (StringUtils.isEmpty(this.node)) {
            loadClientId();
        }
    }

    public void loadClientId() throws Exception {
        Resource resource = new ClassPathResource(PROPERTY_FILE);
        if (!resource.exists()) {
            throw new RuntimeException(PROPERTY_FILE + " 文件不存在!");
        }
        //如果客户端id为空则生成随机id
        String clientId = generateClientId();
        Properties properties = PropertiesLoaderUtils.loadProperties(resource);
        properties.put("client.node", clientId);
        this.setNode(clientId);
        //写回配置文件
        BufferedOutputStream outputStream = FileUtil.getOutputStream(resource.getFile());
        properties.store(outputStream, properties.toString());
    }

    private String generateClientId() {
        return "node:" + NetUtil.getLocalhostStr() + "-" + UUID.fastUUID();
    }
}
