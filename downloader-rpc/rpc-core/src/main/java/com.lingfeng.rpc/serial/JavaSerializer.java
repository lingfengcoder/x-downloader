package com.lingfeng.rpc.serial;


import com.lingfeng.rpc.constant.SerialType;

import java.io.*;

/**
 * <p>
 * </p>
 *
 * @author chenchaobiao
 * @date 2022/5/7 14:53
 * @since 1.0.0
 */
public class JavaSerializer implements ISerializer {

    @Override
    public <T> byte[] serialize(T obj) {
        ByteArrayOutputStream byteArrayOutputStream =
                new ByteArrayOutputStream();
        try {
            ObjectOutputStream outputStream =
                    new ObjectOutputStream(byteArrayOutputStream);

            outputStream.writeObject(obj);
            outputStream.flush();
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        try {
            ObjectInputStream objectInputStream =
                    new ObjectInputStream(byteArrayInputStream);
            return (T) objectInputStream.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public byte getType() {
        return SerialType.JAVA_SERIAL.code();
    }
}