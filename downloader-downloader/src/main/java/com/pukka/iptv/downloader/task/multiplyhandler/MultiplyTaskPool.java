package com.pukka.iptv.downloader.task.multiplyhandler;

import java.util.List;

/**
 * @Author: wz
 * @Date: 2022/1/4 15:26
 * @Description:
 */
public interface MultiplyTaskPool<T> {
    //添加任务
    boolean addTask(T task);

    //获取任务
    List<T> getTask(int count);

    //提交任务
    boolean submitTask();

    //取消任务
    void cancelTask(T task);

    //查询所有的任务个数
    int queryTaskCount();

    //查询正在工作的任务个数
    int queryWorkingTaskCount();

}
