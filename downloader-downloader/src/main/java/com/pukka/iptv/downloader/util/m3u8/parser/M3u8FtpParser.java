package com.pukka.iptv.downloader.util.m3u8.parser;


import cn.hutool.core.lang.Assert;
import com.pukka.iptv.downloader.model.*;
import com.pukka.iptv.downloader.util.FTPUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;

import java.io.IOException;
import java.util.function.Supplier;


/**
 * @Author: wz
 * @Date: 2021/10/26 20:23
 * @Description: FTP解析下载器
 */
@Slf4j
public class M3u8FtpParser extends M3u8Parser {
    private static final Protocol ME = Protocol.FTP;
    private static final M3u8FtpParser instance = new M3u8FtpParser();

    public static M3u8FtpParser instance() {
        return instance;
    }

    @Override
    public M3u8 downloadProxyM3u8(M3u8 m3u8, Proxy proxy) throws Exception {
        log.info("M3u8FtpParser");
        String url = m3u8.getRemoteUrl();
        //设置代理
        Assert.notNull(proxy);
        FTPUrlInfo ftpUrl = FTPUtils.parseFTPUrl(url);
        //入口处登录
        FTPClient client = FTPUtils.ftpProxyLogin(ftpUrl, proxy);
        ConnectHead type = ConnectHead.general(ME).proxy(proxy).ftpClient(client);
        return download(m3u8, () -> type, t -> FTPUtils.logout(t.ftpClient()));
    }

    @Override
    public M3u8 downloadM3u8(M3u8 m3u8) throws Exception {
        //source
        String url = m3u8.getRemoteUrl();
        FTPUrlInfo ftpUrl = FTPUtils.parseFTPUrl(url);
        FTPClient client = FTPUtils.ftpLogin(ftpUrl);
        Supplier<ConnectHead> s = () -> ConnectHead.general(ME).ftpClient(client);
        return download(m3u8, s, t -> FTPUtils.logout(t.ftpClient()));
    }


}
