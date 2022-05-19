package com.lingfeng.biz.downloader.model;


import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @Author: wz
 * @Date: 2021/10/23 20:28
 * @Description: 下载节点的描述
 */
@Setter
@Getter
@Accessors(chain = true)
public class DNode {
    private String instanceId;
    //private QueueInfo queueInfo;
}
