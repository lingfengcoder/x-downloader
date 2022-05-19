package com.lingfeng.biz.server.handler;

import cn.hutool.extra.spring.SpringUtil;
import com.lingfeng.biz.downloader.model.DownloadTask;
import com.lingfeng.biz.downloader.model.TaskFrame;
import com.lingfeng.biz.downloader.model.TaskCmd;
import com.lingfeng.biz.downloader.model.TaskState;
import javafx.concurrent.Task;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import store.StoreApi;

/**
 * @Author: wz
 * @Date: 2022/5/18 19:44
 * @Description:
 */
@Setter
@Getter
@Builder
public class TaskHandler implements Runnable {

    private TaskFrame<DownloadTask> taskFrame;

    @Override
    public void run() {
        String taskId = taskFrame.getTaskId();
        TaskCmd taskCmd = taskFrame.getTaskCmd();
        DownloadTask task = taskFrame.getData();
        StoreApi<DownloadTask> bean = SpringUtil.getBean(StoreApi.class);
        switch (taskCmd) {
            //任务完成 将任务变为完成态
            case TASK_FIN:
                task.setStatus(TaskState.FIN.code());
                //更新任务状态
                bean.updateById(task);
                break;
            //拒绝任务 将任务变为待执行
            case TASK_REJECT:
                task.setStatus(TaskState.WAIT.code());
                //更新任务状态
                bean.updateById(task);
                break;
            //任务失败 考虑是否重试
            case TASK_FAIL:
                break;
        }
    }
}
