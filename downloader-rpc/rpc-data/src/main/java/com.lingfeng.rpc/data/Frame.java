package com.lingfeng.rpc.data;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Setter
@Getter
@ToString
public class Frame<T> implements Serializable {
    private String target;
    private T data;
}