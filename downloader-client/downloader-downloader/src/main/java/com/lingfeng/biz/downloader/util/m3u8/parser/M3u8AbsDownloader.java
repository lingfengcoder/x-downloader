package com.lingfeng.biz.downloader.util.m3u8.parser;

import com.lingfeng.biz.downloader.model.ConnectHead;
import com.lingfeng.biz.downloader.model.Protocol;

import java.io.File;

/**
 * @Author: wz
 * @Date: 2021/11/13 21:50
 * @Description:
 */
public abstract class M3u8AbsDownloader implements M3u8Downloader {

    public static boolean download(String url, File file) throws Exception {
        //获取头部信息
        ConnectHead head = M3u8Parser.getCurrentHead();
        Protocol protocol = head.protocol();
        switch (protocol) {
            case HTTP:
                return HttpDownloader.getInstance().downloadFile(url, file);
            case FTP:
                return FtpDownloader.getInstance().downloadFile(url, file);
        }
        return false;
    }
}
