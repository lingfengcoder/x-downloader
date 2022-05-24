package com.lingfeng.dutation.store.mapper;

import cn.hutool.core.date.SystemClock;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * @Author: wz
 * @Date: 2022/5/24 13:49
 * @Description:
 */
public interface LockMapper {

    @Select("select * from x_lock where `lock` = 'search_lock' for update")
    List<Object> lock();


    //乐观锁 cas
    @Deprecated//暂时没有解决方案
    @Update(" UPDATE x_lock SET ttl = UNIX_TIMESTAMP() +5 WHERE `lock` = 'search_lock' and ttl < UNIX_TIMESTAMP()")
    int tryLock();

}
