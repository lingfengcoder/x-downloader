package com.pukka.iptv.downloader.util.m3u8;


import com.pukka.iptv.downloader.model.M3u8;
import com.pukka.iptv.downloader.model.Protocol;
import com.pukka.iptv.downloader.model.Proxy;
import com.pukka.iptv.downloader.util.UrlParser;
import com.pukka.iptv.downloader.util.m3u8.api.M3u8Api;
import com.pukka.iptv.downloader.util.m3u8.parser.M3u8FtpParser;
import com.pukka.iptv.downloader.util.m3u8.parser.M3u8HttpParser;

import java.util.Objects;


/**
 * @Author: wz
 * @Date: 2021/11/13 21:12
 * @Description: m3u8工具类
 * 地址解析方法:
 * 本地文件前缀: localPrefix = /data/m3u8/xxx/
 * 原始索引地址: url = http://127.0.0.1:8080/a/b/c/index.m3u8
 * 原始索引地址前缀: httpPrefix = http://127.0.0.1:8080/a/b/c/
 * 从index.m3u8读取的index2文件相对坐标: line = m/n/index2.m3u8
 * index2文件绝对坐标: httpPrefix+line = http://127.0.0.1:8080/a/b/c/m/n/index2.m3u8
 * 本地文件需要剔除的前缀:/a/b/c/
 * index本地文件路:  /data/m3u8/xxx/index.m3u8
 * index2本地文件路径: /data/m3u8/xxx/m/n/index2.m3u8
 */
public class M3u8Util implements M3u8Api {

    private static final M3u8Util instance = new M3u8Util();

    public static M3u8Util getInstance() {
        return instance;
    }

    public static void main(String[] args) throws Exception {
        String url="https://hls.videocc.net/4ffae39b72/7/4ffae39b727da1048bc3ea44e8108047_2.m3u8?pid=1639582156168X1131720&device=desktop";
        String local="/data/m3u8/mashibing/";
        String targetUrl="https://hls.videocc.net/4ffae39b72/7/4ffae39b727da1048bc3ea44e8108047_2.m3u8";
        M3u8 m3u8=M3u8.generalM3u8(url,local,targetUrl);
        instance.downloadM3u8(m3u8);
    }
    private M3u8Api matchOneApi(M3u8 m3u8) {
        Protocol protocol = UrlParser.parseProtocol(m3u8.getRemoteUrl());
        if (protocol == null) return null;
        switch (protocol) {
            case HTTP:
                return M3u8HttpParser.instance();
            case FTP:
                return M3u8FtpParser.instance();
        }
        return null;
    }

    /**
     * @Description:
     * @param: [m3u8, proxy]
     * @return: com.pukka.iptv.downloader.model.M3u8
     * @author: wz
     * @date: 2021/11/13 22:39
     */
    @Override
    public M3u8 downloadProxyM3u8(M3u8 m3u8, Proxy proxy) throws Exception {
        return Objects.requireNonNull(matchOneApi(m3u8)).downloadProxyM3u8(m3u8, proxy);
    }

    @Override
    public M3u8 downloadM3u8(M3u8 m3u8) throws Exception {
        return Objects.requireNonNull(matchOneApi(m3u8)).downloadM3u8(m3u8);
    }

    @Override
    public boolean deleteM3u8(M3u8 m3u8) throws Exception {
        return Objects.requireNonNull(matchOneApi(m3u8)).deleteM3u8(m3u8);
    }

    @Override
    public M3u8 getM3u8FinishCount(M3u8 m3u8) throws Exception {
        return Objects.requireNonNull(matchOneApi(m3u8)).getM3u8FinishCount(m3u8);
    }
}
