package com.lingfeng.biz.downloader.util;

import com.lingfeng.biz.downloader.model.ConnectHead;
import com.lingfeng.biz.downloader.model.HttpInfo;
import com.lingfeng.biz.downloader.model.Protocol;
import com.lingfeng.biz.downloader.model.Proxy;
import com.lingfeng.biz.downloader.util.m3u8.api.HttpApi;
import com.lingfeng.biz.downloader.util.m3u8.parser.HttpParser;

import java.util.function.Supplier;

/**
 * @Author: wangbo
 * @Date: 2022/4/2 9:19
 */
public class HttpUtil extends HttpParser implements HttpApi {
    private static final HttpUtil instance = new HttpUtil();
    private static final Protocol ME = Protocol.HTTP;
    private static final ConnectHead HTTP_TYPE = ConnectHead.general(ME);
    public static HttpUtil getInstance() {
        return instance;
    }

    @Override
    public boolean downloadProxyHttp(HttpInfo httpInfo, Proxy proxy) throws Exception {
        Supplier<ConnectHead> s = () -> ConnectHead.general(ME).proxy(proxy);
        download(httpInfo,s,null);
        return true;
    }

    @Override
    public boolean downloadHttp(HttpInfo httpInfo) throws Exception {
        download(httpInfo,()->HTTP_TYPE,null);
        return true;
    }

    @Override
    public boolean deleteHttp(HttpInfo httpInfo) throws Exception {
        return false;
    }
}
