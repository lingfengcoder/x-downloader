package com.pukka.iptv.downloader.util.m3u8.api;

import com.pukka.iptv.downloader.model.M3u8;
import com.pukka.iptv.downloader.model.Proxy;

/**
 * @Author: wz
 * @Date: 2021/11/13 21:31
 * @Description:
 */
public interface M3u8Api {
    M3u8 downloadProxyM3u8(M3u8 m3u8, Proxy proxy) throws Exception;

    M3u8 downloadM3u8(M3u8 m3u8) throws Exception;

    boolean deleteM3u8(M3u8 m3U8) throws Exception;

    M3u8 getM3u8FinishCount(M3u8 m3U8) throws Exception;

}
