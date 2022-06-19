package com.lingfeng.biz.downloader.task.callback;

import com.lingfeng.biz.downloader.model.DTask;
import com.lingfeng.biz.downloader.task.callback.api.TaskNotify;


/**
 * @Author: wz
 * @Date: 2021/10/30 16:06
 * @Description:
 */
public abstract class TaskAbsoluteNotify implements TaskNotify {

    @Override
    public void cancelSuccess(DTask task) {
    }

    @Override
    public void start(DTask task) {

    }

}
