package com.lingfeng.rpc.frame;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * @Author: wz
 * @Date: 2022/5/10 14:56
 * @Description: 简单数据帧模型 对应 SimpleCoder 使用
 */
@Setter
@Getter
@ToString
@Builder
public class SimpleFrame implements Serializable {
    //帧类型     //请求REQUEST((byte) 1), //返回RESPONSE((byte) 2), //心跳HEARTBEAT((byte) 3);
    private byte type;
    private int length;
    private long client;
    private String content;
}
