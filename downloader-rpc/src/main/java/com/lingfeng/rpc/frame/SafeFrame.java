package com.lingfeng.rpc.frame;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @Author: wz
 * @Date: 2022/5/10 14:56
 * @Description: 安全的数据帧模型 对应SafeCoder使用
 * 支持 自动心跳 、多种序列化方式(json java) 、支持传输加密(AES RSA)、自动签名、携带时间戳、防重放攻击，防中间人攻击
 */
@Setter
@Getter
@ToString
@Accessors(chain = true)
public class SafeFrame<T> implements Serializable {
    //帧类型     //请求REQUEST((byte) 1), //返回RESPONSE((byte) 2), //心跳HEARTBEAT((byte) 3);
    private byte cmd;
    //数据(content)序列化类型 JSON_SERIAL JAVA_SERIAL
    private byte serial;
    //加密类型 //明文NONE((byte) 0),//AES AES((byte) 2), //RSA RSA((byte) 3);
    private byte encrypt;
    //时间戳
    private long timestamp;
    //客户端id  -1代表服务端
    private long client;
    //消息签名 MD5 固定32位 timestamp 相当于salt
    private String sign;
    //content 长度
    private int length;
    //内容
    private T content;

}
