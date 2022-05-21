package com.lingfeng.biz.server.dispatcher;

import com.lingfeng.biz.downloader.model.Route;
import com.lingfeng.biz.downloader.util.ListUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Author: wz
 * @Date: 2022/5/19 16:13
 * @Description: 调度器的路由器
 */
@Slf4j
public class DispatcherRouter {

    private final Object lock = new Object();

    private DispatcherRouter() {
    }

    private static final DispatcherRouter instance = new DispatcherRouter();

    public static DispatcherRouter getInstance() {
        return instance;
    }

    //路由表  clientId --> List<Route>
    private final ConcurrentMap<String, List<Route>> routePage = new ConcurrentHashMap<>();

    public int getRouteSize(String key) {
        List<Route> list = routePage.get(key);
        if (list == null || list.isEmpty()) {
            return 0;
        }
        return list.size();
    }


    //添加路由  经过多线程测试 没得问题
    public void addRoute(String key, List<Route> list) {
        boolean second = true;
        if (!routePage.containsKey(key)) {
            final AtomicBoolean first = new AtomicBoolean(false);
            routePage.computeIfAbsent(key, t -> {
                first.set(true);
                return list;
            });
            second = !first.get();
        }
        if (second) {
            routePage.computeIfPresent(key, (k, arr) -> {
                arr.addAll(list);
                return arr;
            });
        }
    }

    //添加路由 经过多线程测试 没得问题
    public void addRoute(String key, Route route) {
        boolean second = true;
        if (!routePage.containsKey(key)) {
            final AtomicBoolean first = new AtomicBoolean(false);
            routePage.computeIfAbsent(key, t -> {
                first.set(true);
                ArrayList<Route> base = new ArrayList<>();
                base.add(route);
                return base;
            });
            second = !first.get();
        }
        if (second) {
            routePage.computeIfPresent(key, (k, arr) -> {
                arr.add(route);
                return arr;
            });
        }
    }

    //获取路由详情
    public List<Route> getRoute(String key) {
        return routePage.get(key);
    }

    public void clearRoute(String key) {
        routePage.remove(key);
    }

    public void removeRouteNode(String key, Route t) {
        List<Route> ts = routePage.get(key);
        if (ts == null || ts.isEmpty()) {
            log.error("[removeRouteNode error]: route is empty {}", key);
            return;
        }
        ListUtils.remove(ts, t);
    }

    //获取路由表
    public ConcurrentMap<String, List<Route>> getRoutePage() {
        return this.routePage;
    }
}
