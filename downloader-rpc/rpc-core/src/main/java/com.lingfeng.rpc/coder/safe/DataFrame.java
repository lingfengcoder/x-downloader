package com.lingfeng.rpc.coder.safe;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * @Author: wz
 * @Date: 2022/5/11 16:17
 * @Description: 序列化的基础帧
 */
@Setter
@Getter
@ToString
public class DataFrame<T extends Serializable> implements Serializable {
    private String classname;
    private T data;
}
