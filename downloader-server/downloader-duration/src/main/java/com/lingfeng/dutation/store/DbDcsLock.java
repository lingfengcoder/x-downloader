package com.lingfeng.dutation.store;

import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;
import com.lingfeng.dutation.store.mapper.LockMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.annotation.Resource;
import java.util.function.Function;

/**
 * @Author: wz
 * @Date: 2022/5/18 15:47
 * @Description:
 */
@Slf4j
@Component
@MybatisPlusTest
public class DbDcsLock {

    @Resource
    private LockMapper lockMapper;
    @Autowired
    private DataSourceTransactionManager transactionManager;

    public <T, R> R lock(Function<T, R> consumer) {
        return lock(consumer, -1);
    }

    public <T, R> R lock(Function<T, R> consumer, int ttl) {
        log.info("db dsc-lock begin");
        // 2.获取事务定义
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        if (ttl > 0) def.setTimeout(ttl);
        // 3.设置事务隔离级别，开启新事务
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        // 4.获得事务状态
        TransactionStatus status = transactionManager.getTransaction(def);
        long begin = System.currentTimeMillis();
        try {
            //select * from x_lock where `lock` = 'search_lock' for update
            lockMapper.lock();
            //业务代码
            return consumer.apply(null);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            //回滚事务
            transactionManager.rollback(status);
        } finally {
            try {
                //提交事务
                transactionManager.commit(status);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            log.info("db dsc-lock end 执行总耗时:{}ms", (System.currentTimeMillis() - begin));
        }
        return null;
    }

}
