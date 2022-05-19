package com.lingfeng.biz.downloader.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @Author: wz
 * @Date: 2022/5/19 16:25
 * @Description:
 */
public class ListUtils {

    //获取count个item，并减少list
    public static <T> List<T> subList(List<T> list, int count) {
        Iterator<T> iterator = list.iterator();
        List<T> result = new ArrayList<>(count);
        while (iterator.hasNext()) {
            if (count == 0) break;
            result.add(iterator.next());
            iterator.remove();
            count--;
        }
        return result;
    }

    //删除指定元素
    public static <T> boolean remove(List<T> list, T t) {
        Iterator<T> iterator = list.iterator();
        while (iterator.hasNext()) {
            T next = iterator.next();
            if (next.equals(t)) {
                iterator.remove();
                return true;
            }
        }
        return false;
    }

}
