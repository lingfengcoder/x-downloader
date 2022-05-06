package com.pukka.iptv.downloader.mq.config;

import com.alibaba.fastjson.JSONObject;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Base64;


@Slf4j
public class RestHttp {

    private static RestTemplate restTemplate;

    static {
        HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        httpRequestFactory.setConnectionRequestTimeout(3000);
        httpRequestFactory.setConnectTimeout(3000);
        httpRequestFactory.setReadTimeout(3000);
        restTemplate = new RestTemplate(httpRequestFactory);
    }

    public static RestTemplate getRestHttp() {
        return restTemplate;
    }

    //带Authorization认证信息的http请求
    public static JSONObject httpAuth(String url, String username, String pwd) {
        RestTemplate restTemplate = getRestHttp();//new RestTemplate();
        //认证的账号和密码
        String authentication = username + ":" + pwd;
        HttpHeaders headers = new HttpHeaders();
        //在请求头信息中携带Basic认证信息(这里才是实际Basic认证传递用户名密码的方式)
        headers.set("Authorization", "Basic " + Base64.getEncoder().encodeToString(authentication.getBytes()));

        //设置编码格式
        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(type);
        //过滤掉账号认证失败的时候抛出的401异常
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                log.error("url:{}", url);
                if (response.getRawStatusCode() != 401) {
                    super.handleError(response);
                }
            }
        });
        //发送请求
        ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.GET,
                new HttpEntity<byte[]>(headers), String.class);
        return JSONObject.parseObject(exchange.getBody());
    }

}
