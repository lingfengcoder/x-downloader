package com.lingfeng.biz.downloader.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Getter
@Setter
//@Configuration
//@RefreshScope//动态刷新bean
public class ResultConfig {

    @Value("${downloader.callback.secondtime:360}")
    private Long secondCallbackTime;

    @Value("${downloader.callback.thirdtime:3600}")
    private Long thirdCallbackTime;


    @Value("${downloader.callback.firsttime:7200}")
    private Long firstCallbackTime;

    @Value("${downloader.callback.resultQueue:downloader-callback-result-queue}")
    private String resultQueue;

    @Value("${downloader.callback.resultExchange:downloader-callback-error-exchange}")
    private String resultExchange;

    @Value("${downloader.callback.resultRoutekey:downloader-callback-resultRoutekey}")
    private String resultRoutekey;


}
