package com.pukka.iptv.downloader.nacos;


import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;


/**
 * @Description: 带感知服务 的http客户端
 * @author: wz
 * @date: 2021/11/9 15:34
 */
@Slf4j
@Order(1)
@Configuration
@EnableDiscoveryClient
public class RestHttpConfig {

    @LoadBalanced
    @Bean("balanceHttp")
    public RestTemplate balanceHttp() {
        HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        httpRequestFactory.setConnectionRequestTimeout(3000);
        httpRequestFactory.setConnectTimeout(3000);
        httpRequestFactory.setReadTimeout(3000);
        return new RestTemplate(httpRequestFactory);
    }

    @Bean("restHttp")
    public static RestTemplate restHttp() {
        HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        httpRequestFactory.setConnectionRequestTimeout(3000);
        httpRequestFactory.setConnectTimeout(3000);
        httpRequestFactory.setReadTimeout(3000);
        return new RestTemplate(httpRequestFactory);
    }


}
