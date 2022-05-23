package com.lingfeng.biz.server.cache;

import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

/**
 * @Author: wz
 * @Date: 2022/5/19 15:01
 * @Description: 采用高低水位的方式 来控制缓冲队列的消费速度
 */
@Setter
@Getter
public class WaterCacheQueue<T> {
    private volatile int lowWaterLevel;
    private volatile int highWaterLevel;
    private volatile int max;//暂时无用
    private Queue<T> cache;//如果需要多线程

    public WaterCacheQueue(int lowWaterLevel, int highWaterLevel, Queue<T> cache) {
        this.lowWaterLevel = lowWaterLevel;
        this.highWaterLevel = highWaterLevel;
        this.cache = cache;
    }

    public void modifyLowWaterLevel(int low) {
        this.lowWaterLevel = low;
    }

    public void modifyHighWaterLevel(int high) {
        this.highWaterLevel = high;
    }

    public void init(List<T> list) {
        for (T t : list) {
            if (!isEnough()) {
                add(t);
            }
        }
    }

    public boolean add(T t) {
        if (cache.size() < highWaterLevel) {
            return cache.add(t);
        }
        return false;
    }

    //忽略高水位的增加
    public boolean addMust(T t) {
        return cache.add(t);
    }

    //低于低水位认为需要获取数据
    public boolean isHungry() {
        return cache.size() < lowWaterLevel;
    }

    //是否饱了（到达高水位）
    public boolean isEnough() {
        return cache.size() >= highWaterLevel;
    }

    //获取 高水位与实际的差值，（用于一次性吃饱:将数据填充到高水位）
    public int diff() {
        return highWaterLevel - cache.size();
    }

    //一次一次一直add到高水位
    public void addUntilFull(Supplier<T> addFunc) {
        while (!isEnough()) {
            T t = addFunc.get();
            add(t);
        }
    }

    //一次add一批，直到高水位
    public List<T> addSomeUntilFull(Supplier<List<T>> addFunc) {
        while (!isEnough()) {
            List<T> list = addFunc.get();
            //如果获取不到数据了就执行
            if (list == null || list.isEmpty()) {
                break;
            }
            int failNum = -1;
            for (int i = 0; i < list.size(); i++) {
                if (!isEnough()) {
                    boolean ok = add(list.get(i));
                    if (!ok) {
                        //添加失败的
                        failNum = i;
                        break;
                    }
                }
            }
            if (failNum >= 0) {
                //跳出主循环 返回剩余没有添加成功的
                return list.subList(failNum, list.size());
            }
        }
        return null;
    }

    //获取一个
    public T poll() {
        return cache.poll();
    }

    //获取指定个数的
    public List<T> pollSome(int count) {
        List<T> tmp = new ArrayList<>();
        while (count > 0) {
            T t = poll();
            if (t == null) {
                break;
            }
            tmp.add(t);
            --count;
        }
        return tmp;
    }

    public int size() {
        return cache.size();
    }
}
