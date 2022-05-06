package com.pukka.iptv.downloader.task.multiplyhandler;


/**
 * @Author: wz
 * @Date: 2022/1/4 15:26
 * @Description:
 */
public interface MultiplyTaskPool<T> {
    //添加任务
    boolean submitTask(T task);

    //获取任务
    int getAliveTaskCount();


    int getLimit();

    void setLimit(int x);

    //取消任务
    void cancelTask(T task);

}
