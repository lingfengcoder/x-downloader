package com.lingfeng.biz.server.dispatcher;

import com.lingfeng.biz.server.model.NodeClient;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: wz
 * @Date: 2022/5/19 16:32
 * @Description: 客户端节点 存储
 */

public class NodeClientGroup {
    private NodeClientGroup() {
    }

    private static final NodeClientGroup instance = new NodeClientGroup();

    public static NodeClientGroup getInstance() {
        return instance;
    }

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

    public NodeClient getClient(String clientId) {
        return clients.get(clientId);
    }
}
