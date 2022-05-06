package com.pukka.iptv.downloader.nacos.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author WangBO
 * @email wangbo@cjyun.org
 * @date 2021/10/15
 */
@Getter
@Setter
@ToString
public class NacosHost {
    private String instanceId;
    private String ip;
    private int port;
    private boolean healthy;
    private boolean enabled;
    private boolean ephemeral;
    private String clusterName;
}
