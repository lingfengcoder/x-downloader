package com.lingfeng.rpc.server;

import com.lingfeng.rpc.client.NettyClient;
import com.lingfeng.rpc.model.Message;
import com.lingfeng.rpc.model.MessageType;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: wz
 * @Date: 2022/5/9 20:46
 * @Description:
 */
@Slf4j
public class MessageDispatcher {
    public static void dispatcher(Message<Object> msg) {
        MessageType type = MessageType.trans(msg.getType());
        int clientId = msg.getClientId();
        switch (type) {
            //正常传输信息
            case MSG:
                log.info("[netty server ] 收到客户端{}的消息：{}", clientId, msg);
                break;
            //服务端要求关闭客户端
            case CLOSE_CLIENT:
                break;
            //服务端将会关闭
            case CLIENT_CLOSING:
                log.info("[netty server ] 收到客户端{}的 关闭请求：{}", clientId, msg);
                break;
        }
    }
}