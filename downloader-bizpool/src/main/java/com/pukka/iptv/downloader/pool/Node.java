package com.pukka.iptv.downloader.pool;

import lombok.ToString;

import java.lang.ref.WeakReference;
import java.util.Objects;

/**
 * @Author: wz
 * @Date: 2021/12/15 11:32
 * @Description: 连接池节点 注意本类不能对外提供set/get方法，防止外部修改，仅提供外部使用的几个API方法
 */
@ToString
public class Node<K extends Key<K>, C> {
    //node对应的唯一标识
    protected volatile K key;
    //真正客户端
    protected volatile C client;
    //持有node的线程
    protected volatile WeakReference<Thread> holder;
    //是否空闲
    protected volatile boolean free;
    //是否关闭
    protected volatile boolean close;
    //上次使用的时间
    protected volatile Long lastUsedTime;
    //尝试关闭的次数
    protected volatile int tryCloseCount;

    private Node() {
    }


    public static <K extends Key<K>, C> Node<K, C> node(K key, C client) {
        Node<K, C> node = new Node<>();
        node.client = client;
        node.key = key;
        node.free = false;
        node.close = false;
        return node;
    }

    public C getClient() {
        return client;
    }

    public K getKey() {
        //使用clone防止外部直接修改内部属性，造成混乱
        return key.cloneMe();
    }

    public void clear() {
        //help GC
        //this.key = null;
        this.client = null;
        this.lastUsedTime = null;
        if (this.holder != null) {
            this.holder.clear();
            this.holder = null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node<?, ?> node = (Node<?, ?>) o;
        return Objects.equals(key, node.key) &&
                Objects.equals(client, node.client);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, client);
    }
}
