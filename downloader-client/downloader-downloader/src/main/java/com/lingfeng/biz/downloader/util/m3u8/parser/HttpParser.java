package com.lingfeng.biz.downloader.util.m3u8.parser;

import com.lingfeng.biz.downloader.model.ConnectHead;
import com.lingfeng.biz.downloader.model.HttpInfo;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @Author: wangbo
 * @Date: 2022/4/2 9:31
 */
@Slf4j
public class HttpParser  {


    //下载协议（方式）的threadLocal
    private static final ThreadLocal<ConnectHead> headThreadLocal = new ThreadLocal<>();


    //获取当前头部信息
    public static ConnectHead getCurrentHead() {
        return headThreadLocal.get();
    }

    //前置处理器
    private static void preProcess(Supplier<ConnectHead> supplier) {
        headThreadLocal.set(supplier.get());
    }

    protected HttpInfo download(HttpInfo httpInfo, Supplier<ConnectHead> preFunc, Consumer<ConnectHead> endFunc) {
        try {
            preProcess(preFunc);
            return loopDownload(httpInfo);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
        }
        return null;
    }


    protected static HttpInfo loopDownload(HttpInfo httpInfo) throws Exception {
        File file = M3u8Parser.makeLocalFile(httpInfo.getLocalFilePath());
        HttpDownloader.getInstance().downloadFile(httpInfo.getRemoteUrl(), file);
        return httpInfo;
    }
}
