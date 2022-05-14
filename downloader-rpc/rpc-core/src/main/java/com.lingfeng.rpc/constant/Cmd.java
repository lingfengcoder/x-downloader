package com.lingfeng.rpc.constant;

/**
 * <p>
 * </p>
 * <p>
 * 数据帧类型
 */
public enum Cmd {

    //请求
    REQUEST((byte) 1),
    //返回
    RESPONSE((byte) 2),
    //心跳
    HEARTBEAT((byte) 3),
    //认证
    AUTH((byte) 4);


    private final byte code;

    private Cmd(byte code) {
        this.code = code;
    }

    public byte code() {
        return this.code;
    }

    public static Cmd findByCode(int code) {
        for (Cmd msgType : Cmd.values()) {
            if (msgType.code() == code) {
                return msgType;
            }
        }
        return null;
    }
}