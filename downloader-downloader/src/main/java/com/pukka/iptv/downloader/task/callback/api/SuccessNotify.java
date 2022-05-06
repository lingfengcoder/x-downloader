package com.pukka.iptv.downloader.task.callback.api;

import com.pukka.iptv.downloader.model.DownloadTask;

/**
 * @Author: wz
 * @Date: 2021/10/25 14:15
 * @Description:
 */
@FunctionalInterface
public interface SuccessNotify {
    //下载完成
    void success(DownloadTask task);
}
