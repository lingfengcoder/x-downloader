package com.lingfeng.rpc;

import java.util.function.Consumer;

/**
 * @Author: wz
 * @Date: 2022/5/7 17:39
 * @Description:
 */
public interface EndPointInterface<T> {
    public void start(final String address, final int port, final String appname, final String accessToken);

    public void stop() throws Exception;

    void callback(Consumer<T> callback);
}
