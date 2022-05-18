package com.lingfeng.biz.downloader.pool;

import java.io.Serializable;

public interface Key<K> extends Serializable {

    //获取单个key最大的连接数
    int getLimit();

    K setLimit(int x);

    //获取key的名字
    String getName();

    K cloneMe();
}
