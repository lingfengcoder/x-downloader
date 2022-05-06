package com.pukka.iptv.downloader.task.callback.api;

import com.pukka.iptv.downloader.model.DownloadTask;
import com.pukka.iptv.downloader.model.TaskMsg;

/**
 * @Author: wz
 * @Date: 2021/10/25 14:16
 * @Description:
 */
@FunctionalInterface
public interface FailedNotify {
    //下载完成
    void failed(DownloadTask task, String msg);
}
