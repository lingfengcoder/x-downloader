package com.pukka.iptv.downloader.mq.event;

import com.pukka.iptv.downloader.mq.pool.RabbitMqPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;

/**
 * @Author: wz
 * @Date: 2021/12/27 17:32
 * @Description:
 */
@Slf4j
@Component
public class EventListener {
    @PreDestroy
    protected void destroy() {
        log.warn(" system destroy ");
        RabbitMqPool.me().destroy();
    }
}
