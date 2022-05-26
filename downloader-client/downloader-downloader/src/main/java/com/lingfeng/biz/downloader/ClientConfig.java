package com.lingfeng.biz.downloader;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.net.NetUtil;
import com.lingfeng.rpc.model.Address;
import com.lingfeng.rpc.util.StringUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.util.Properties;

/**
 * @Author: wz
 * @Date: 2022/5/26 14:46
 * @Description:
 */
@Slf4j
@Setter
@Getter
@Configuration
@ComponentScan(basePackages = "com.lingfeng.biz.downloader")
public class ClientConfig {
    private volatile String clientId;
    private Address address;

    public void loadClientConfig() throws Exception {
        log.info("loadClientConfig:开始加载配置client.yml");
        Resource resource = new ClassPathResource("client.yml");
        if (!resource.exists()) {
            throw new RuntimeException("client.yml 文件不存在!");
        }
        Properties props;
        props = PropertiesLoaderUtils.loadProperties(resource);
        clientId = props.getProperty("node-id");
        String server = props.getProperty("server");
        String[] serverSp = server.split(":");
        address = new Address(serverSp[0], Integer.parseInt(serverSp[1]));

        if (StringUtils.isEmpty(clientId)) {
            clientId = loadClientId(resource);
        }
    }

    private String loadClientId(Resource resource) throws IOException {
        //如果客户端id为空则生成随机id
        String clientId = generateClientId();
        Properties tmp = new Properties();
        tmp.put("node-id", clientId);
        //回写 目的就是为了固定node的id
        PropertiesLoaderUtils.fillProperties(tmp, resource);
        return clientId;
    }

    private String generateClientId() {
        return "download-node:" + NetUtil.getLocalhostStr() + UUID.fastUUID();
    }
}
