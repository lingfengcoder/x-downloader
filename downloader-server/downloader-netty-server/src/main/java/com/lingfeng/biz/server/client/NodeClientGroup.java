package com.lingfeng.biz.server.client;

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

    public void removeNodeClientByChannelId(String channelId) {
        NodeClient client = getClientByChannelId(channelId);
        if (client != null) {
            removeNodeClient(client.getClientId());
        }
    }

    public Collection<NodeClient> getClients() {
        return clients.values();
    }

    public NodeClient getClient(String clientId) {
        return clients.get(clientId);
    }

    public NodeClient getClientByChannelId(String channelId) {
        Collection<NodeClient> values = clients.values();
        for (NodeClient client : values) {
            if (client.getChannel() != null) {
                if (client.getChannel().id().asLongText().equals(channelId)) {
                    return client;
                }
            }
            //如果channel被回收了，可以通过channelId识别
            else if (client.getChannelId().equals(channelId)) {
                return client;
            }
        }
        return null;
    }
}
