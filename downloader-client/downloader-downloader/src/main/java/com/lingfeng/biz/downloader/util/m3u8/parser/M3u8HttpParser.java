package com.lingfeng.biz.downloader.util.m3u8.parser;


import cn.hutool.core.lang.Assert;

import com.lingfeng.biz.downloader.model.*;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;


/**
 * @Author: wz
 * @Date: 2021/10/26 20:23
 * @Description:
 */
@Slf4j
public class M3u8HttpParser extends M3u8Parser {
    private static final Protocol ME = Protocol.HTTP;
    private static final M3u8HttpParser instance = new M3u8HttpParser();
    private static final ConnectHead HTTP_TYPE = ConnectHead.general(ME);

    public static M3u8HttpParser instance() {
        return instance;
    }

    @Override
    public M3u8 downloadProxyM3u8(M3u8 m3u8, Proxy proxy) {
        log.info("M3u8HttpParser");
        Assert.notNull(proxy);
        //设置代理
        Supplier<ConnectHead> s = () -> ConnectHead.general(ME).proxy(proxy);
        return download(m3u8, s, null);
    }

    @Override
    public M3u8 downloadM3u8(M3u8 m3u8) {
        return download(m3u8, () -> HTTP_TYPE, null);
    }
}
