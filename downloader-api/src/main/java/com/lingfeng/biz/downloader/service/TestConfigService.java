package com.lingfeng.biz.downloader.service;

import com.lingfeng.biz.downloader.config.DemoConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class TestConfigService {
    @Autowired
    private DemoConfig config;
    @Autowired
    private TestConfigService configService;
    @Autowired
    private ApplicationContext applicationContext;

    @PostConstruct
    public void inits() {
        new Thread(() -> {
            while (true) {
                String age = configService.config.getAge();
                String name = configService.config.getName();
                log.info("name ={} age={} ", name, age);

                try {
                    TimeUnit.MILLISECONDS.sleep(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
        new Thread(()->{
            while (true){
                try {
                    TimeUnit.MILLISECONDS.sleep(5000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                log.info("开始触发监听事件");
              //  applicationContext.publishEvent(ApplicationEnvironmentPreparedEvent.class);
            }
        }).start();
    }
}
