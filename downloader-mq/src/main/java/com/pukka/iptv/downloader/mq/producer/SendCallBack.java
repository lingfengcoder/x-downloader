package com.pukka.iptv.downloader.mq.producer;

/**
 * @Auther: wz
 * @Date: 2021/10/18 14:21
 * @Description:
 */
public interface SendCallBack {
    boolean sendSuccess(long deliveryTag);

    boolean sendFail(long deliveryTag);
}
