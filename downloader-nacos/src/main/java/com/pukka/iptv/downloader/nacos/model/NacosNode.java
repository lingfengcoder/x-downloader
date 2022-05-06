package com.pukka.iptv.downloader.nacos.model;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * @author WangBO
 * @email wangbo@cjyun.org
 * @date 2021/10/15
 */
@Getter
@Setter
@ToString
public class NacosNode {
    private String dom;
    private int cacheMillis;
    private boolean useSpecifiedURL;
    private List<NacosHost> hosts;
    private String checksum;
    private long lastRefTime;
    private String env;
    private String clusters;
}

