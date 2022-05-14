package com.lingfeng.rpc.model;


import lombok.Getter;

/**
 * @Author: wz
 * @Date: 2022/5/9 20:07
 * @Description:
 */

@Getter
public enum MessageType {
    PING(1), PONG(2), MSG(3),
    //要求客户端关闭
    CLOSE_CLIENT(4),
    //服务端关闭中 在data 中提示服务端多久后将会关闭
    SERVER_CLOSING(5),
    //客户端将会关闭 在data 中提示服务端多久后将会关闭
    CLIENT_CLOSING(6);
    private final int code;

    MessageType(int code) {
        this.code = code;
    }

    public static MessageType trans(int code) {
        for (MessageType item : MessageType.values()) {
            if (item.code == code) {
                return item;
            }
        }
        return null;
    }
}
