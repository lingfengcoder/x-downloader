package com.lingfeng.biz.downloader.task.multiplyhandler;

import com.lingfeng.biz.downloader.model.DTask;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * @Author: wz
 * @Date: 2022/3/1 15:51
 * @Description:
 */
@Slf4j
public class Test {

    public static void main(String[] args) {
        DemoMultiplyTaskHandler handler = new DemoMultiplyTaskHandler();
        int x = 1000;
        List<DTask> data = new ArrayList<>();
        for (int i = 0; i < x; i++) {
            data.add(new DTask().setTaskId(i + ""));
        }


        new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log.info("10S 后将并发数修改为4");
            handler.setLimit(4);
            try {
                TimeUnit.SECONDS.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log.info("20S 后将并发数修改为8");
            handler.setLimit(8);
            try {
                TimeUnit.SECONDS.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log.info("20S 后将并发数修改为1");
            handler.setLimit(1);
        }).start();


        Iterator<DTask> iterator = data.iterator();
        DTask next = null;
        while (iterator.hasNext()) {
            if (next == null) {
                next = iterator.next();
            }
            if (handler.submitTask(next)) {
                iterator.remove();
                next = null;
            } else {
                try {
                   // log.info("主线程等待2S后再尝试提交任务");
                    TimeUnit.MILLISECONDS.sleep(1);
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        log.info("任务全部执行完毕");
    }

}
