package com.lingfeng.biz.downloader.enums;


public enum ResultNumberEnums
{
    ONE(1, "第一次重试"),
    TWO(2, "第二次重试"),
    THREE(3, "第三次重试");

    private Integer code;
    private String value;

    ResultNumberEnums(Integer code, String value) {
        this.code = code;
        this.value = value;
    }

    public Integer getCode() {
        return code;
    }

    public String getValue() {
        return value;
    }
    public static ResultNumberEnums trans(int i) {
        for (ResultNumberEnums value : ResultNumberEnums.values()) {
            if (value.code == i) {
                return value;
            }
        }
        return null;
    }
}
