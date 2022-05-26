package com.lingfeng.biz.server.schedule;

import cn.hutool.extra.spring.SpringUtil;
import com.lingfeng.biz.server.handler.taskhandler.TaskHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

import java.util.Map;

/**
 * @Author: wz
 * @Date: 2022/5/26 10:45
 * @Description:
 */
@Slf4j
public class DispatcherJob implements Runnable {
    public DispatcherJob(boolean enable) {
        this.enable = enable;
    }

    private volatile boolean enable;

    @Override
    public void run() {
        if (enable) {
            doWork();
        }
    }


    private void doWork() {
        ApplicationContext app = SpringUtil.getApplicationContext();
        if (app != null) {
            log.info("下载模块 调度中心开始执行调度");
            //找出容器中所有的处理器
            Map<String, TaskHandler> taskHandler = app.getBeansOfType(TaskHandler.class);
            for (Map.Entry<String, TaskHandler> item : taskHandler.entrySet()) {
                try {
                    item.getValue().handler();
                    //executor.execute(() -> item.getValue().handler());
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }
        }
    }
}
