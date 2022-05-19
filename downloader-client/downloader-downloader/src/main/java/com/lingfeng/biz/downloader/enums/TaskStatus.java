package com.lingfeng.biz.downloader.enums;

/**
 * @Author jxm
 * @Date 2021-10-20 11:27
 */
public enum TaskStatus {
    WAIT(0, "待下载"),
    ING(1, "下载中"),
    SUCCESS(2, "下载成功"),
    FAIL(3, "下载失败");

    private int code;
    private String value;

    TaskStatus(int code, String value) {
        this.code = code;
        this.value = value;
    }

    public int getCode() {
        return code;
    }

    public String getValue() {
        return value;
    }
}
