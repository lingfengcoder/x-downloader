package com.lingfeng.rpc.store;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: wz
 * @Date: 2022/5/9 20:29
 * @Description:
 */
public class MessageStore {
    private final static MessageStore instance = new MessageStore();
    private final ConcurrentHashMap<Integer, Long> msgStore = new ConcurrentHashMap<>();

    public static Long getClientMsgSeq(int clientId) {
        ConcurrentHashMap<Integer, Long> store = instance.msgStore;
        Long seq = store.get(clientId);
        if (seq != null) {
            store.put(clientId, seq + 1);
            return seq + 1;
        } else {
            store.put(clientId, 1L);
        }
        return 1L;
    }

    public static void removeClientMsgSeq(int clientId) {
        ConcurrentHashMap<Integer, Long> store = instance.msgStore;
        Long seq = store.get(clientId);
        if (seq != null) {
            store.remove(clientId);
        }
    }
}
