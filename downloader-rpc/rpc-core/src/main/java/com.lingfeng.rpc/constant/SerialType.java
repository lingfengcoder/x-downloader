package com.lingfeng.rpc.constant;

/**
 * <p>
 * </p>
 * <p>
 * 序列化类型
 */
public enum SerialType {

    //
    STRING_SERIAL((byte) -1),
    JSON_SERIAL((byte) 0),
    JAVA_SERIAL((byte) 1);

    private final byte code;

    SerialType(byte code) {
        this.code = code;
    }

    public byte code() {
        return this.code;
    }
}