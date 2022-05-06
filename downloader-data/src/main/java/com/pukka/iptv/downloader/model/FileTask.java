package com.pukka.iptv.downloader.model;

import lombok.*;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

/**
 * @Author jxm
 * @Date 2021-10-14 11:35
 * @Description 文件任务信息
 */
@Getter
@Setter
@Accessors(chain = true)
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class FileTask implements Serializable {
    //1异步下载 2.实时下载
    private int async;
    // 通知地址
    @NotBlank(message = "通知地址为null")
    private String notifyUrl;
    // 文件code
    @NotBlank(message = "文件code为null")
    private String fileCode;
    // 源地址 可能为ftp也可能是http
    @NotBlank(message = "源地址为null")
    private String sourceUrl;
    // 目标路径
    @NotBlank(message = "目标FTP路径为null")
    private String targetUrl;
    // 文件类型 1:视频 2:图片
    @NotNull(message = "文件类型为null")
    private Integer fileType;
    @NotNull(message = "存储id为null")
    private Long storeId;
    //代理
    private Proxy proxy;
    // 任务id
    private Long id;
    // nacos组件唯一id
    private String taskServerInstanceId;
    //文件名称
    private String filename;
    // 重试次数
    private int retryCount;
    // 处理优先级
    private int priority;
    // 下载任务状态
    private Integer status;
    // 下载开始时间
    private Date startTime;
    // 下载结束时间
    private Date finishTime;
    // 下载耗时(格式： xx小时xx分xx秒)
    private String timeConsuming;
    //目标文件全路径
    private String fileLocalPath;
}
