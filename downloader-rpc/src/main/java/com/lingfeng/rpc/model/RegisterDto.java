package com.lingfeng.rpc.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * @Author: wz
 * @Date: 2022/5/7 15:20
 * @Description:
 */
@Setter
@Getter
@AllArgsConstructor
public class RegisterDto {
    private String appname;
    private String address;
}
