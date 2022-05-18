package com.lingfeng.biz.downloader.util.m3u8.parser;

import cn.hutool.core.lang.Assert;
import com.lingfeng.biz.downloader.config.HttpClient;
import com.lingfeng.biz.downloader.model.ConnectHead;
import com.lingfeng.biz.downloader.model.Proxy;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;

/**
 * @Author: wz
 * @Date: 2021/11/13 18:00
 * @Description: http下载器
 */
class HttpDownloader extends M3u8AbsDownloader {
    private final static HttpDownloader instance = new HttpDownloader();

    public static HttpDownloader getInstance() {
        return instance;
    }

    //返回http客户端
    private static RestTemplate getHttp() {
        return HttpClient.getRestHttp();
    }

    //返回带代理的http客户端
    private static RestTemplate getProxyHttp(Proxy proxy) {
        return HttpClient.getProxyHttp(proxy);
    }

    @Override
    public boolean downloadFile(String url, File file) throws Exception {
        Assert.notNull(file);
        Assert.notNull(url);
        //获取头部信息
        ConnectHead head = new ConnectHead();
        if (M3u8Parser.getCurrentHead() == null) {
            head = HttpParser.getCurrentHead();
        } else {
            head = M3u8Parser.getCurrentHead();
        }
        Proxy proxy = head.proxy();
        RestTemplate httpClient = proxy == null ? getHttp() : getProxyHttp(proxy);
        Assert.notNull(httpClient);
        RequestCallback callback = request -> request.getHeaders()
                .setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file);
            FileOutputStream finalout = outputStream;
            httpClient.execute(url, HttpMethod.GET, callback, resp -> {
                FileCopyUtils.copy(resp.getBody(), finalout);
                //log.info("下载完成:{}", resp.getStatusCode());
                return true;
            });
        } finally {
            closeStream(outputStream);
        }
        return true;
    }
}
