package com.lingfeng.rpc.serial;

import java.nio.charset.StandardCharsets;

import static com.lingfeng.rpc.constant.SerialType.STRING_SERIAL;

/**
 * @Author: wz
 * @Date: 2022/5/11 18:06
 * @Description:
 */
public class StringSerializer implements ISerializer {
    @Override
    public <T> byte[] serialize(T obj) {
        String tmp = (obj.toString());
        return tmp.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) {
        return (T) new String(data, StandardCharsets.UTF_8);
    }

    @Override
    public byte getType() {
        return STRING_SERIAL.code();
    }
}
