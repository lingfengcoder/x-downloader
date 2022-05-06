package com.pukka.iptv.downloader.nacos;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * @Author: wz
 * @Date: 2021/10/15 14:25
 * @Description:
 */
@Getter
@Setter
@Configuration
@Order(1)
//@Accessors(fluent = true)
//@NacosPropertySource(dataId = "iptv-downloader-dev.yaml", groupId = "iptv", autoRefreshed = true)
@RefreshScope//动态刷新bean
public class NacosConfig {

    @Value("${downloader.node.servername}")
    private String serverName;

    //iptv-downloader-${spring.profiles.active}.${spring.cloud.nacos.config.file-extension}
    @Value("iptv-downloader-${spring.profiles.active}.${spring.cloud.nacos.config.file-extension}")
    private String dataId;

    //nacos的配置
    @Value("${nacos.server-addr}")
    private String serverAddr;
    @Value("${nacos.group}")
    private String group;
    @Value("${nacos.namespace}")
    private String namespace;
    @Value("${server.port}")
    private Integer port;

}
