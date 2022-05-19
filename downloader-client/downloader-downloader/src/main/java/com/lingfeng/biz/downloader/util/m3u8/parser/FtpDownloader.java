package com.lingfeng.biz.downloader.util.m3u8.parser;

import cn.hutool.core.lang.Assert;
import com.lingfeng.biz.downloader.model.ConnectHead;
import com.lingfeng.biz.downloader.model.FTPUrlInfo;
import com.lingfeng.biz.downloader.model.Proxy;
import com.lingfeng.biz.downloader.util.FTPUtils;
import org.apache.commons.net.ftp.FTPClient;
import java.io.File;

/**
 * @Author: wz
 * @Date: 2021/11/13 20:34
 * @Description:
 */
class FtpDownloader extends M3u8AbsDownloader {
    private final static FtpDownloader instance = new FtpDownloader();

    public static FtpDownloader getInstance() {
        return instance;
    }

    @Override
    public boolean downloadFile(String url, File file) throws Exception {
        //获取头部信息
        ConnectHead currentHead = M3u8Parser.getCurrentHead();
        FTPClient client = currentHead.ftpClient();
        Assert.notNull(client);
        FTPUtils.download(client, url, file);
        return true;
    }


    //todo
    private void getFtpClientByPool(String url, ConnectHead currentHead) throws Exception {
        FTPUrlInfo ftpUrl = FTPUtils.parseFTPUrl(url);
        FTPClient client = currentHead.ftpClient();
        Proxy proxy = currentHead.proxy();
        if (proxy != null) {
            client = FTPUtils.ftpProxyLogin(ftpUrl, proxy);
        } else {
            client = FTPUtils.ftpLogin(ftpUrl);
        }
    }
}
