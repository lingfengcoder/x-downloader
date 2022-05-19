package com.lingfeng.biz.downloader.enums;

/**
 * @Author jxm
 * @Date 2021-10-18 10:13
 */
public enum FileType {
    VIDEO(1, "视频"),
    PICTURE(2, "图片");

    private int code;
    private String value;

    FileType(int code, String value) {
        this.code = code;
        this.value = value;
    }

    public int getCode() {
        return code;
    }

    public String getValue() {
        return value;
    }

    public static FileType trans(int i) {
        for (FileType value : FileType.values()) {
            if (value.code == i) {
                return value;
            }
        }
        return null;
    }
}
