package com.lingfeng.biz.downloader.task.callback;

import com.lingfeng.biz.downloader.model.DownloadTask;
import com.lingfeng.biz.downloader.task.callback.api.TaskNotify;


/**
 * @Author: wz
 * @Date: 2021/10/30 16:06
 * @Description:
 */
public abstract class TaskAbsoluteNotify implements TaskNotify {

    @Override
    public void cancelSuccess(DownloadTask task) {
    }

    @Override
    public void start(DownloadTask task) {

    }

}
