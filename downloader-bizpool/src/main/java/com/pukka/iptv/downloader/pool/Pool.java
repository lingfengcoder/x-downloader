package com.pukka.iptv.downloader.pool;

/**
 * @Author: wz
 * @Date: 2021/12/13 16:54
 * @Description: 连接池模型 具有 线程阻塞获取/空闲自动定时关闭/动态调整等功能
 */
public interface Pool<K extends Key, N, C> {

    //阻塞式获取一个连接
    N pickBlock(K k);

    //带超时时间的获取
    N pickBlock(K k, long timeout);

    //不阻塞获取，获取不到直接返回
    N pickNonBlock(K k);

    //归还一个连接
    void back(N n);

    //归还并关闭一个连接
    void backClose(N n);

    //根据配置调整连接池
    void refreshPoolConfig(C c);

    //定时清理
    void scheduleClear();

    //测试连接是否可用
    boolean nodeIsOpen(N n);

    //关闭连接
    void closePool();

    //开启连接
    void openPool();

    void destroy();

}
