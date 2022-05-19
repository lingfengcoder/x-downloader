package com.lingfeng.biz.server.dispatcher;

import com.lingfeng.biz.server.model.NodeClient;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: wz
 * @Date: 2022/5/19 16:32
 * @Description: 客户端节点 存储
 */
@Component
public class NodeClientStore {
    private final ConcurrentHashMap<String, NodeClient> clients = new ConcurrentHashMap<>();

    public void addNodeClient(NodeClient client) {
        clients.put(client.getClientId(), client);
    }

    public void removeNodeClient(String clientId) {
        clients.remove(clientId);
    }

    public Collection<NodeClient> getClients() {
        return clients.values();
    }
}
