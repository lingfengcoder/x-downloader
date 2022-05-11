package com.lingfeng.rpc.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * @Author: wz
 * @Date: 2022/5/11 16:39
 * @Description:
 */
@Setter
@Getter
@ToString
public class TempData implements Serializable {
    private long id;
    private String name;
}
