package com.pukka.iptv.downloader.mq.consumer;

import com.pukka.iptv.downloader.mq.model.MsgTask;

/**
 * @Author: wz
 * @Date: 2021/10/19 21:32
 * @Description:
 */
@FunctionalInterface
public interface ConsumerNotify {
    boolean notify(MsgTask msgTask);
}
