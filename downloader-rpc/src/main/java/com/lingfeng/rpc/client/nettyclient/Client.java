package com.lingfeng.rpc.client.nettyclient;

public interface Client {

    int state();

    void start();

    void restart();

    void close();
}
