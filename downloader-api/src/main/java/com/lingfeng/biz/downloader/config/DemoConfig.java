package com.lingfeng.biz.downloader.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "biz")
public class DemoConfig {
    @Value("name")
    private String name;
    @Value("age")
    private String age;
}
