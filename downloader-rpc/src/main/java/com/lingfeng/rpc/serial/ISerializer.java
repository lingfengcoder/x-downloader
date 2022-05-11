package com.lingfeng.rpc.serial;

/**
 * <p>
 * </p>
 *
 * @author chenchaobiao
 * @date 2022/5/7 14:53
 * @since 1.0.0
 */
public interface ISerializer {

    <T> byte[] serialize(T obj);

    <T> T deserialize(byte[] data,Class<T> clazz);

    byte getType();
}