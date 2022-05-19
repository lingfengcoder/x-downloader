package com.lingfeng.biz.downloader.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * @Author: wz
 * @Date: 2022/5/18 19:47
 * @Description: 基础帧 主要用来 注册、退出 等
 */
@Setter
@Getter
@ToString
@Builder
public class BasicFrame<T> implements Serializable {
    //目标方法
    private String target;
    //任务详情
    private T data;
    //客户端id 要求：全局唯一，重启后不变化
    private String clientId;
    //指令集
    private BasicCmd cmd;
}
