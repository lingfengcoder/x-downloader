package com.lingfeng.dutation.store;

import java.util.List;

/**
 * @Author: wz
 * @Date: 2022/5/18 15:45
 * @Description:
 */
public interface StoreApi<T> {
    boolean save(T t);

    List<T> query(int limit);

    List<T> queryAndModify(int limit, int srcState, int tarState);

    boolean updateById(T t);
}
