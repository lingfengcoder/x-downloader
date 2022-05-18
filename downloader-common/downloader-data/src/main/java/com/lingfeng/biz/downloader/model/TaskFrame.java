package com.lingfeng.biz.downloader.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * @Author: wz
 * @Date: 2022/5/18 19:47
 * @Description:
 */
@Setter
@Getter
@ToString
public class TaskFrame<T> implements Serializable {
    private String target;
    private T t;
    private String taskId;
    private TaskState taskState;
}
