package com.lingfeng.biz.downloader.config;

import com.lingfeng.biz.downloader.model.LocalStoreInfo;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @Author: wz
 * @Date: 2021/10/21 11:40
 * @Description: 本地存储配置映射
 */
@Getter
@Setter
@Configuration
@RefreshScope//动态刷新bean
@ConfigurationProperties(prefix = "downloader.local-store")
public class StoreConfig {
    //ftp挂载本地的 本地路径前缀
    //@Value("${downloader.localStore.defaultPrefix}")
    private String defaultPrefix;
    //@Value("${downloader.localStore.nodes}")
    private List<LocalStoreInfo> nodes;

    public String getPrefixById(long id) {
        if (nodes != null) {
            for (LocalStoreInfo property : nodes) {
                if (property.getId() == id) {
                    return property.getPrefix();
                }
            }
        }
        return defaultPrefix;
    }


}
