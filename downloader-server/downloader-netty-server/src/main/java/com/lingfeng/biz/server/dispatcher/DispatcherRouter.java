package com.lingfeng.biz.server.dispatcher;

import com.lingfeng.biz.downloader.model.Route;
import com.lingfeng.biz.downloader.util.ListUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

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
public class DispatcherRouter {
    @Autowired
    private NodeClientStore nodeClientStore;
    //路由表  clientId --> List<Route>
    private final ConcurrentMap<String, List<Route>> routePage = new ConcurrentHashMap<>();

    public int getRouteSize(String key) {
        List<Route> list = routePage.get(key);
        if (list == null || list.isEmpty()) {
            return 0;
        }
        return list.size();
    }

    //添加路由
    public void addRoute(String key, List<Route> list) {
        if (routePage.containsKey(key)) {
            List<Route> old = routePage.get(key);
            if (old == null) {
                routePage.put(key, list);
            } else {
                old.addAll(list);
            }
        } else {
            routePage.put(key, list);
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
