package com.lingfeng.biz.downloader.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

/**
 * @Author: wz
 * @Date: 2022/5/26 10:20
 * @Description:
 */
@Setter
@Getter
@Accessors(fluent = true)
public class RouteResult<I, T> {
    //路由结果
    private Map<I, List<T>> routeMap;
    //总条数
    private int count;

    public RouteResult(Map<I, List<T>> routeMap, int count) {
        this.routeMap = routeMap;
        this.count = count;
    }

    public RouteResult() {
    }
}
