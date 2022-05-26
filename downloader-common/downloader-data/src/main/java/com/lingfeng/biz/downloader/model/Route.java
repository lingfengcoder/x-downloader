package com.lingfeng.biz.downloader.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @Author: wz
 * @Date: 2022/5/19 17:29
 * @Description:
 */
@Setter
@Getter
@ToString
public class Route<T> {
    private String target;
    private String id;
    private T data;
}
