package com.lingfeng.biz.downloader.task.callback.api;

/**
 * @Author: wz
 * @Date: 2021/10/30 16:05
 * @Description:
 */
public interface TaskNotify extends StartNotify, SuccessNotify, FailedNotify, CancelNotify, FinishedNotify {
}
