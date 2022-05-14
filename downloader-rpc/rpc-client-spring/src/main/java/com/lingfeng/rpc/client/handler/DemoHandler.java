package com.lingfeng.rpc.client.handler;

import com.lingfeng.rpc.client.ann.RpcComponent;
import com.lingfeng.rpc.client.ann.RpcHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@RpcComponent
@Slf4j
public class DemoHandler {

    @RpcHandler("bbq")
    public Object bbq(String param) {
        log.info(" client get msg = " + param);
        return "I love you too  --bbq";
    }


    @RpcHandler("complexParam")
    public Object complexParam(Map<String, Long> param) {

        log.info(" client get a map data = {}", param);

        return "map is OK  --bbq";
    }
}
