package com.lingfeng.biz.server.dispatcher;

import com.lingfeng.biz.downloader.model.Route;
import com.lingfeng.biz.downloader.util.ListUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @Author: wz
 * @Date: 2022/5/19 16:13
 * @Description: 调度器的路由器
 */
@Slf4j
public class DispatcherRouter<T> {

    private final Object lock = new Object();

    private DispatcherRouter() {
    }

    private static final DispatcherRouter instance = new DispatcherRouter();

    public static DispatcherRouter getInstance() {
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
    public void addRoute(String key, List<Route<T>> list) {
        List<Route<T>> old = routePage.get(key);
        if (old == null) {
            synchronized (lock) {
                old = routePage.get(key);
                if (old == null) {
                    routePage.put(key, list);
                } else {
                    old.addAll(list);
                }
            }
        } else {
            synchronized (lock) {
                old.addAll(list);
            }
        }
    }

    //添加路由
    public void addRoute(String key, Route route) {
        List<Route<T>> old = routePage.get(key);
        if (old == null) {
            synchronized (lock) {
                old = routePage.get(key);
                if (old == null) {
                    routePage.put(key, Arrays.asList(route));
                } else {
                    old.add(route);
                }
            }
        } else {
            synchronized (lock) {
                old.add(route);
            }
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
