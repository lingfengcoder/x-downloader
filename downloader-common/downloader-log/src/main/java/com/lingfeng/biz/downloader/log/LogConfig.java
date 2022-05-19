package com.lingfeng.biz.downloader.log;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * @Author: wz
 * @Date: 2021/11/13 17:17
 * @Description:
 */
@Getter
@Setter
@Configuration
@Order(1)
@RefreshScope//动态刷新bean
public class LogConfig {

    //是否打印日志
    @Value("${downloader.log.enable}")
    private Boolean logEnable;
}
