package com.pukka.iptv.downloader.mq.producer;

/**
 * @Auther: wz
 * @Date: 2021/10/18 14:21
 * @Description:
 */
@FunctionalInterface
public interface SendWaitCallBack {
    boolean callback();
}
