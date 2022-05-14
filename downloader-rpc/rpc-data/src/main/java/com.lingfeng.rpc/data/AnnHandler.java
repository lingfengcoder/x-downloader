package com.lingfeng.rpc.data;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.lang.reflect.Method;

@Setter
@Getter
@ToString
public class AnnHandler {
    private String name;
    private Method method;
    private Object bean;
}
