package com.lingfeng.biz.downloader.model;

/**
 * @Author: wz
 * @Date: 2022/5/18 15:32
 * @Description:
 */
public enum TaskCmd {
    NEW_TASK(1),//新任务
    TASK_FIN(2),//任务完成
    TASK_FAIL(3),// 任务失败
    TASK_REJECT(4);//拒绝任务
    private int code;

    TaskCmd(int code) {
        this.code = code;
    }

    public static TaskCmd trans(int state) {
        for (TaskCmd value : TaskCmd.values()) {
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
