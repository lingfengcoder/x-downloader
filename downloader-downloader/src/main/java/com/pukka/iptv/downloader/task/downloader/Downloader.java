package com.pukka.iptv.downloader.task.downloader;


/**
 * @Author: wz
 * @Date: 2021/10/25 10:23
 * @Description:
 */
public interface Downloader<T> {

    //前置处理
    void preHandler(T task);

    //同步下载文件 不进入队列 直接当前线程进行下载
    boolean download(T task);

    //后置处理
    void postHandler(T task);

    //查询下载任务
    T selectTask(T task);

}
