package com.lingfeng.biz.downloader.task.callback.api;

import com.lingfeng.biz.downloader.model.DTask;

/**
 * @Author: wz
 * @Date: 2021/10/25 14:16
 * @Description:
 */
@FunctionalInterface
public interface FailedNotify {
    //下载完成
    void failed(DTask task, String msg);
}
