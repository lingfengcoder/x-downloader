package com.lingfeng.biz.downloader.model;

/**
 * @Author: wz
 * @Date: 2022/5/18 15:32
 * @Description:
 */
public enum TaskState {
    NEW_TASK,//新任务
    TASK_FIN,//任务完成
    TASK_FAIL,// 任务失败
    TASK_REJECT//拒绝任务
}
