package com.lingfeng.rpc.data;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Map;

@Setter
@Getter
@ToString
public class RpcInvokeBean implements Serializable {

    private String methodName;

    private Class<?>[] parameterTypes;

    private Object[] arguments;

    private Map<String, String> attachments;
}
