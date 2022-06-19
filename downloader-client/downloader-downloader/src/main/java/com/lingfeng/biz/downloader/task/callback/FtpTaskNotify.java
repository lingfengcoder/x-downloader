package com.lingfeng.biz.downloader.task.callback;

import com.lingfeng.biz.downloader.model.DTask;
import com.lingfeng.biz.downloader.task.process.post.TaskFailedProcess;
import com.lingfeng.biz.downloader.task.process.post.TaskFinishedProcess;
import com.lingfeng.biz.downloader.task.process.post.FtpPostProcess;
import com.lingfeng.biz.downloader.task.process.post.TaskStartedProcess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @Author: wz
 * @Date: 2021/10/30 16:11
 * @Description: ftp通知器
 */
@Component
public class FtpTaskNotify extends TaskAbsoluteNotify {
    @Autowired
    private FtpPostProcess ftpPostProcess;
    @Autowired
    private TaskFailedProcess taskFailedProcess;
    @Autowired
    private TaskStartedProcess taskStartedProcess;
    @Autowired
    private TaskFinishedProcess taskFinishedProcess;

    @Override
    public void start(DTask task) {
        taskStartedProcess.handler(task, true);
    }

    @Override
    public void success(DTask task) {
        ftpPostProcess.handler(task, true);
    }

    @Override
    public void failed(DTask task, String msg) {
        taskFailedProcess.handler(task, true);
    }

    @Override
    public void finish(DTask task) {
        taskFinishedProcess.handler(task, false);
    }
}
