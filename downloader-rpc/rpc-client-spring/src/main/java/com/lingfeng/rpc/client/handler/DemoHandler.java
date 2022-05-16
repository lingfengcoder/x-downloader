package com.lingfeng.rpc.client.handler;

import com.lingfeng.rpc.ann.RpcComponent;
import com.lingfeng.rpc.ann.RpcHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@RpcComponent
@Slf4j
public class DemoHandler {

    @RpcHandler("bbq")
    public Object bbq(String param) throws InterruptedException {
        Thread thread = Thread.currentThread();
        TimeUnit.MILLISECONDS.sleep(500);
        log.info(" client get msg ={}  thread={}", param, thread);
        return "I love you too  --bbq";
    }


    @RpcHandler("complexParam")
    public Object complexParam(Map<String, Long> param) {
        Thread thread = Thread.currentThread();
        log.info(" client get a map data = {} ,thread={}", param, thread);

        return "map is OK  --bbq";
    }
}
