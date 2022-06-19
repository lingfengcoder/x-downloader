package com.lingfeng.biz.downloader.model;

import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * @Author: wz
 * @Date: 2022/5/18 19:47
 * @Description: 基础帧 主要用来 注册、退出 等
 */
@Builder
@Accessors(chain = true)
@Data
public class BasicFrame<T> implements Comparable<BasicFrame>, Serializable {
    //目标方法
    private String target;
    //任务详情
    private T data;
    //客户端id 要求：全局唯一，重启后不变化
    private String clientId;
    //指令集
    private BasicCmd cmd;
    private Integer id;
    /**
     * 原始url
     */
    private String url;
    /**
     * 下载协议 1-http 2-ftp 3-m3u8
     */
    private String type;
    /**
     * 下载次数
     */
    private Integer redoCount;
    /**
     * 下载节点
     */
    private String node;
    /**
     * 下载状态
     */
    private Integer status;
    /**
     * 下载耗时
     */
    private Long costTime;
    /**
     *
     */
    private Date createTime;
    /**
     *
     */
    private Date updateTime;

    @Override
    public int compareTo(BasicFrame o) {
        return 0;
    }
}
