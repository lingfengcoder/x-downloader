package com.lingfeng.rpc.client;

public interface Client {

    int state();

    void start();

    void restart();

    void close();
}
