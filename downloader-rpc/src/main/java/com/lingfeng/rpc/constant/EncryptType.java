package com.lingfeng.rpc.constant;

/**
 * <p>
 * </p>
 * <p>
 * 加密类型
 */
public enum EncryptType {

    //明文
    NONE((byte) 0),
    //AES
    AES((byte) 2),
    //RSA
    RSA((byte) 3);


    private final byte code;

    private EncryptType(byte code) {
        this.code = code;
    }

    public byte code() {
        return this.code;
    }

    public static EncryptType findByCode(int code) {
        for (EncryptType msgType : EncryptType.values()) {
            if (msgType.code() == code) {
                return msgType;
            }
        }
        return null;
    }
}