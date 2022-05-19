package com.lingfeng.biz.downloader.task.callback.api;

import com.lingfeng.biz.downloader.model.DownloadTask;

/**
 * @Author: wz
 * @Date: 2021/10/25 14:16
 * @Description:
 */
@FunctionalInterface
public interface FinishedNotify {
    //取消下载成功
    void finish(DownloadTask task);
}
