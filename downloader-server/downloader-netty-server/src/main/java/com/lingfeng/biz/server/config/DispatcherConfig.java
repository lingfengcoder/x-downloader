package com.lingfeng.biz.server.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * @Author: wz
 * @Date: 2021/10/15 14:25
 * @Description:
 */
@Getter
@Setter
@Configuration
//@Accessors(fluent = true)
//@NacosPropertySource(dataId = "biz-downloader-dev.yaml", groupId = "biz", autoRefreshed = true)
//@RefreshScope//动态刷新bean
@PropertySource("classpath:server.properties")
public class DispatcherConfig {


    //是否开始调度
    @Value("${downloader.dispatcher.enable}")
    private Boolean enable;
    //#是否开启自动定时执行
    @Value("${downloader.dispatcher.enableSchedule}")
    private Boolean enableSchedule;
    @Value("${downloader.dispatcher.intervalTime:1000}")
    private Long intervalTime;

    //任务队列缓冲任务数，影响发送速率，数字越小发送越快
    @Value("${downloader.dispatcher.cacheTaskLen}")
    private Integer cacheTaskLen;

    //    //单节点队列最大 正常消息数(影响下载执行队列长度)
    @Value("${downloader.dispatcher.nodeTaskLimit}")
    private Integer taskLimit;
//    //单节点队列最大 重试消息数
//    @Value("${downloader.node.retryLenLimit}")
//    private Integer retryLenLimit;

}
