package com.lingfeng.biz.downloader.util.m3u8.api;

import com.lingfeng.biz.downloader.model.HttpInfo;
import com.lingfeng.biz.downloader.model.Proxy;

/**
 * @Author: wz
 * @Date: 2021/11/13 21:31
 * @Description:
 */
public interface HttpApi {
    boolean downloadProxyHttp(HttpInfo httpInfo, Proxy proxy) throws Exception;

    boolean downloadHttp(HttpInfo httpInfo) throws Exception;

    boolean deleteHttp(HttpInfo httpInfo) throws Exception;
}
