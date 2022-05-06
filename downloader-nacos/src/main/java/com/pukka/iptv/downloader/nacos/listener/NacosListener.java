package com.pukka.iptv.downloader.nacos.listener;


import cn.hutool.extra.spring.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @Author: wz
 * @Date: 2021/11/9 22:18
 * @Description: nacos配置刷新事件的监听器
 */
@Component
@Slf4j
public class NacosListener implements ApplicationListener<RefreshScopeRefreshedEvent> {

    private final static BlockingQueue<NacosNotify> staticBeanQueue = new LinkedBlockingQueue<>();

    public static void register(NacosNotify bean) {
        staticBeanQueue.add(bean);
    }

    public static void out(NacosNotify bean) {
        staticBeanQueue.remove(bean);
    }

    @Override
    public void onApplicationEvent(RefreshScopeRefreshedEvent e) {
        //spring bean的通知
        Map<String, NacosNotify> beans = SpringUtil.getBeansOfType(NacosNotify.class);
        Set<Map.Entry<String, NacosNotify>> all = beans.entrySet();
        for (Map.Entry<String, NacosNotify> item : all) {
            notify(item.getValue());
        }

        //静态对象的通知
        staticBeanQueue.forEach(this::notify);
    }

    private void notify(NacosNotify nacosNotify) {
        try {
            // executor.submit(() -> nacosNotify.configRefreshEvent());
            nacosNotify.configRefreshEvent();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

}
