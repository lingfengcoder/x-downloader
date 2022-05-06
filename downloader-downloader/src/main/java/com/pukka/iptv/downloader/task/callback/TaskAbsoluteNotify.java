package com.pukka.iptv.downloader.task.callback;

import com.pukka.iptv.downloader.model.DownloadTask;
import com.pukka.iptv.downloader.model.TaskMsg;
import com.pukka.iptv.downloader.task.callback.api.TaskNotify;


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
