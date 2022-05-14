package com.lingfeng.rpc.client;

import com.lingfeng.rpc.client.nettyclient.NettyClient;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: wz
 * @Date: 2022/5/9 20:46
 * @Description:
 */
@Slf4j
public class MessageDispatcher {
    public static void dispatcher(Channel channel, NettyClient client, Object msg) {
        //MessageType type = MessageType.trans(msg.getType());
        long clientId = client.getClientId();
        log.info("[netty client id: {}] 收到服务端{}的消息：{}", clientId, channel.remoteAddress(), msg);
//        switch (type) {
//            //正常传输信息
//            case MSG:
//                log.info("[netty client id: {}] 收到服务端{}的消息：{}", clientId, channel.remoteAddress(), msg);
//                break;
//            //服务端要求关闭客户端
//            case CLOSE_CLIENT:
//                client.close();
//                break;
//            //服务端将会关闭
//            case SERVER_CLOSING:
//                break;
//        }
    }
}
