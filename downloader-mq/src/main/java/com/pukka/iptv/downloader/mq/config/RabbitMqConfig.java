package com.pukka.iptv.downloader.mq.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * @Author: wz
 * @Date: 2021/10/18 10:05
 * @Description:
 */
@Order(1)
@Setter
@Getter
@RefreshScope//动态刷新bean
//@Accessors(fluent = true)
@Configuration
@ToString
public class RabbitMqConfig {
    @Value("${spring.rabbitmq.addresses}")
    private String addresses;
    @Value("${spring.rabbitmq.username}")
    private String username;
    @Value("${spring.rabbitmq.password}")
    private String password;
    @Value("${spring.rabbitmq.virtual-host}")
    private String vhost;
}
