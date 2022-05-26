package com.lingfeng.biz.downloader.config;

import com.lingfeng.biz.downloader.model.FtpAuth;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * @Author: wz
 * @Date: 2021/11/18 16:35
 * @Description: FTP用户名和目录的映射关系
 */
@Slf4j
@Getter
@Setter
//@Configuration
//@RefreshScope//动态刷新bean
//@ConfigurationProperties(prefix = "downloader")
public class FtpConfig {
    private final static String DEFAULT_LOCAL_PATH = "default";
    //ftp授权用户信息
    private FtpAuth ftpAuth;
    //用户名和本地相对路径的映射关系
    private Map<String, String> ftpUserPath;

    //ftp用户名转换为路径
    public String tranPathByUsername(String username) {
        String s = ftpUserPath.get(username);
        return s != DEFAULT_LOCAL_PATH ? s : username;
    }
}
