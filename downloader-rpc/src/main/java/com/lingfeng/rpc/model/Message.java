package com.lingfeng.rpc.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @Author: wz
 * @Date: 2022/5/9 20:07
 * @Description:
 */
@Setter
@Getter
@Builder
@ToString
public class Message<T> {
    private long seq;
    private int clientId;
    private T data;
    private int type;
    private long time;
}
