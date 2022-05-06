package com.pukka.iptv.downloader.task.callback.api;

import com.pukka.iptv.downloader.model.DownloadTask;

/**
 * @Author: wz
 * @Date: 2021/10/25 14:16
 * @Description:
 */
@FunctionalInterface
public interface StartNotify {
    //开始下载
    void start(DownloadTask task);
}
