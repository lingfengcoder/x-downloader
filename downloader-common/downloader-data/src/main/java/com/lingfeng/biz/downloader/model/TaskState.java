package com.lingfeng.biz.downloader.model;

/**
 * @Author: wz
 * @Date: 2022/5/18 15:32
 * @Description:
 */
public enum TaskState {
    WAIT(1),//待执行
    DOING(2),//执行中
    FIN(3),// 任务完成
    ERROR(4);//任务异常
    private int code;

    TaskState(int code) {
        this.code = code;
    }

    public static TaskState trans(int state) {
        for (TaskState value : TaskState.values()) {
            if (value.code == state) {
                return value;
            }
        }
        return null;
    }


    public int code() {
        return code;
    }
}
