package com.lingfeng.biz.server.model;

import io.netty.channel.Channel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @Author: wz
 * @Date: 2022/5/18 15:23
 * @Description: 下载节点客户端
 */
@Setter
@Getter
@ToString
public class DownloaderClient {
    private String id;
    private String host;
    private int port;
    private long registerTime;
    private long updateTime;
    private Channel channel;
}
