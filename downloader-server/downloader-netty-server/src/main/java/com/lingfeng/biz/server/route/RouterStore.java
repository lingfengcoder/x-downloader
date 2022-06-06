package com.lingfeng.biz.server.route;

import com.lingfeng.biz.downloader.model.Route;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Author: wz
 * @Date: 2022/5/19 16:13
 * @Description: 路由器存储
 */
@Slf4j
public class RouterStore<T> {

    private final Object lock = new Object();

    private RouterStore() {
    }

    private static final RouterStore instance = new RouterStore();

    public static RouterStore getInstance() {
        return instance;
    }

    //路由表  clientId --> List<Route<T>>
    private final ConcurrentMap<String, List<Route<T>>> routePage = new ConcurrentHashMap<>();

    public int getRouteSize(String key) {
        List<Route<T>> list = routePage.get(key);
        if (list == null || list.isEmpty()) {
            return 0;
        }
        return list.size();
    }

    //添加路由
    //添加路由  经过多线程测试 没得问题
    public void addRoute(String key, List<Route<T>> list) {
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
    public void addRoute(String key, Route<T> route) {
        boolean second = true;
        if (!routePage.containsKey(key)) {
            final AtomicBoolean first = new AtomicBoolean(false);
            routePage.computeIfAbsent(key, t -> {
                first.set(true);
                ArrayList<Route<T>> base = new ArrayList<>();
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
    public List<Route<T>> getRoute(String key) {
        return routePage.get(key);
    }

    public Route<T> getRoute(String key, String routeId) {
        List<Route<T>> routes = routePage.get(key);
        for (Route<T> route : routes) {
            if (route.getId().equals(routeId)) {
                return route;
            }
        }
        return null;
    }

    public void clearRoute(String key) {
        routePage.remove(key);
    }

    public boolean removeRouteNode(String key, Route<T> t) {
        List<Route<T>> ts = routePage.get(key);
        if (ts == null || ts.isEmpty()) {
            log.error("[removeRouteNode error]: route is empty {}", key);
            return false;
        }
        return ts.remove(t);
    }

    public boolean removeRouteNode(String key, String routeId) {
        List<Route<T>> ts = routePage.get(key);
        if (ts == null || ts.isEmpty()) {
            log.error("[removeRouteNode error]: route is empty {}", key);
            return false;
        }
        for (Route<T> t : ts) {
            if (t.getId().equals(routeId)) {
                ts.remove(t);
                return true;
            }
        }
        return false;
    }

    //获取路由表
    public ConcurrentMap<String, List<Route<T>>> getRoutePage() {
        return this.routePage;
    }
}
