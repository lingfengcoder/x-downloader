package com.lingfeng.biz.downloader.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

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
@RefreshScope//动态刷新bean
public class NodeConfig {
    //单节点消费开关
    @Value("${downloader.node.enable}")
    private boolean enable;
    //内部定时任务是否开启
    @Value("${downloader.node.enableSchedule}")
    private boolean enableSchedule;
    //单节点允许同时下载(影响下载执行并发数)
    @Value("${downloader.node.channelLimit}")
    private Integer channelLimit;
    //单节点允许同时下载(影响下载执行并发数)
    @Value("${downloader.node.concurrentLimit}")
    private Integer concurrentLimit;
    //单节点队列最大 正常消息数(影响下载执行队列长度)
    @Value("${downloader.node.queueLenLimit}")
    private Integer queueLenLimit;
    //队列发送限制数
    @Value("${downloader.node.sendLimit}")
    private Integer sendLimit;
    //单节点队列最大 重试消息数
    @Value("${downloader.node.retryLenLimit}")
    private Integer retryLenLimit;
    //失败的任务重试的次数
    @Value("${downloader.node.failedRetryCount}")
    private Integer failedRetryCount;
    //下载完成后是否上传FTP
    @Value("${downloader.node.autoUploadFtp}")
    private Boolean autoUploadFtp;
    //临时下载文件的存活时间
    @Value("${downloader.node.tmpIndex.liveTime}")
    private Long tmpIndexLiveTime;

    //临时下载文件的存活时间
    // @Value("${downloader.node.tmpIndex.timeCron}")
    // private String tmpIndexTimeCron;
}
