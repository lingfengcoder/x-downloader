package com.lingfeng.biz.downloader.model;

/**
 * @Author: wz
 * @Date: 2022/5/18 15:32
 * @Description:
 */
public enum BasicCmd {
    REG(1),//注册
    CLOSE(2);//关闭
    private int code;

    BasicCmd(int code) {
        this.code = code;
    }

    public static BasicCmd trans(int state) {
        for (BasicCmd value : BasicCmd.values()) {
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
