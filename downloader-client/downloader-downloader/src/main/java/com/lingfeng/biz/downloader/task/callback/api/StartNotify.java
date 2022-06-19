package com.lingfeng.biz.downloader.task.callback.api;

import com.lingfeng.biz.downloader.model.DTask;

/**
 * @Author: wz
 * @Date: 2021/10/25 14:16
 * @Description:
 */
@FunctionalInterface
public interface StartNotify {
    //开始下载
    void start(DTask task);
}
