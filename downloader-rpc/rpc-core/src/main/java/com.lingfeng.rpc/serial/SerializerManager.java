package com.lingfeng.rpc.serial;

import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * 实现对序列化机制的管理
 * </p>
 *
 * @author chenchaobiao
 * @date 2022/5/7 14:53
 * @since 1.0.0
 */
public class SerializerManager {

    private final static ConcurrentHashMap<Byte, ISerializer> serializers = new ConcurrentHashMap<>();

    static {
        ISerializer jsonSerializer = new JsonSerializer();
        ISerializer javaSerializer = new JavaSerializer();
        ISerializer stringSerializer = new StringSerializer();
        serializers.put(jsonSerializer.getType(), jsonSerializer);
        serializers.put(javaSerializer.getType(), javaSerializer);
        serializers.put(stringSerializer.getType(), stringSerializer);
    }

    public static ISerializer getSerializer(byte key) {
        ISerializer serializer = serializers.get(key);
        if (serializer == null) {
            return new JavaSerializer();
        }
        return serializer;
    }
}