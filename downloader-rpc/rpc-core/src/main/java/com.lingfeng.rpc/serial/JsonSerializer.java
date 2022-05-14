package com.lingfeng.rpc.serial;


import com.lingfeng.rpc.constant.SerialType;
import com.lingfeng.rpc.util.GsonTool;

import java.nio.charset.StandardCharsets;

/**
 * <p>
 * </p>
 *
 * @author chenchaobiao
 * @date 2022/5/7 14:53
 * @since 1.0.0
 */
public class JsonSerializer implements ISerializer {
    @Override
    public <T> byte[] serialize(T obj) {
        return GsonTool.toJson(obj).getBytes(StandardCharsets.UTF_8);
//        return null;//JSON.toJSONString(obj).getBytes();
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) {
        return GsonTool.fromJson(new String(data), clazz);
//        return null;//JSON.parseObject(new String(data),clazz);
    }

    @Override
    public byte getType() {
        return SerialType.JSON_SERIAL.code();
    }
}