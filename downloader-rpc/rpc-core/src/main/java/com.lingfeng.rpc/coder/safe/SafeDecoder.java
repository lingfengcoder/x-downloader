package com.lingfeng.rpc.coder.safe;

import com.lingfeng.rpc.constant.SerialType;
import com.lingfeng.rpc.frame.SafeFrame;
import com.lingfeng.rpc.model.Address;
import com.lingfeng.rpc.serial.ISerializer;
import com.lingfeng.rpc.serial.SerializerManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: wz
 * @Date: 2022/5/10 14:53
 * @Description:
 */
@Slf4j
public class SafeDecoder extends ByteToMessageDecoder {


    /**
     * maxFrameLength      帧的最大长度
     * lengthFieldOffset   length字段偏移的地址
     * lengthFieldLength   length字段所占的字节长
     * lengthAdjustment    修改帧数据长度字段中定义的值，可以为负数 因为有时候我们习惯把头部记入长度,若为负数,则说明要推后多少个字段
     * initialBytesToStrip 解析时候跳过多少个长度
     * failFast            为true，当frame长度超过maxFrameLength时立即报TooLongFrameException异常，
     * 为false，读取完整个帧再报异
     * // super(9999, 1, 4, 0, 0);
     */


    //   帧类型     //请求REQUEST((byte) 1), //返回RESPONSE((byte) 2), //心跳HEARTBEAT((byte) 3);
    //    private byte type;
    //    //数据(content)序列化类型 JSON_SERIAL JAVA_SERIAL
    //    private byte serial;
    //    //加密类型 //明文NONE((byte) 0),//AES AES((byte) 2), //RSA RSA((byte) 3);
    //    private byte encrypt;
    //    //时间戳
    //    private long timestamp;
    //    //content 长度
    //    //消息签名 MD5 固定32位 timestamp 相当于salt
    //    private String sign;
    //    //客户端id  -1代表服务端
    //    private long client;

    //    private int length;
    //
    //    private String content;
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        try {
            //在这里调用父类的方法
            if (in == null) {
                return;
            }
            //读取type字段
            byte cmd = in.readByte();// 1
            byte serial = in.readByte();// 1
            byte encrypt = in.readByte();// 1
            long timestamp = in.readLong();// 8
            long clientId = in.readLong();//8
            //签名读取
            byte[] signByte = new byte[32];
            in.readBytes(signByte, 0, 32);
            String sign = new String(signByte, StandardCharsets.UTF_8);//32

            //读取length字段
            int length = in.readInt();//4
            if (in.readableBytes() != length) {
                throw new RuntimeException("长度与标记不符");
            }
            //如果读取到; 说明前面都是类名
            String className = readClass(in);
            //数据所属的clazz
            Class<?> bodyClazz = cacheClazz(className);
            //读取body
            byte[] bytes = new byte[in.readableBytes()];
            in.readBytes(bytes);
            //.content(new String(bytes, StandardCharsets.UTF_8)).build();
            //根绝数据帧获取序列化工具
            ISerializer serializer = SerializerManager.getSerializer(serial);
            SafeFrame<Object> safeFrame = new SafeFrame<>();
            Object deserialize = serializer.deserialize(bytes, bodyClazz);
            safeFrame.setContent(deserialize);
            safeFrame.setCmd(cmd)
                    .setSerial(serial)
                    .setEncrypt(encrypt)
                    .setTimestamp(timestamp)
                    .setClient(clientId)
                    .setSign(sign)
                    .setLength(length);
            out.add(safeFrame);
        } catch (
                Exception e) {
            log.error(e.getMessage(), e);
        }

    }

    //对java class 类的缓存
    private final static ConcurrentHashMap<String, Class<?>> classCache = new ConcurrentHashMap<>();

    private Class<?> cacheClazz(String className) {
        Class<?> cache = classCache.get(className);
        if (cache == null) {
            Class<?> clazz;
            try {
                clazz = Class.forName(className);
                classCache.put(className, clazz);
                cache = clazz;
            } catch (ClassNotFoundException e) {
                log.error(e.getMessage(), e);
            }
        }
        return cache;
    }

    // 数据结构com.lingfeng.rpc.frame.Frame ; class data
    private String readClass(ByteBuf in) {
        byte b;
        List<Byte> tmp = new ArrayList<>();
        // 读取到 ; 分隔符
        while ((b = in.readByte()) != 59) {
            tmp.add(b);
        }
        byte[] array = new byte[tmp.size()];
        for (int i1 = 0; i1 < tmp.size(); i1++) {
            array[i1] = tmp.get(i1);
        }
        return new String(array, StandardCharsets.UTF_8);
    }

    public static void main(String[] args) throws ClassNotFoundException {
        byte[] bytes = ";".getBytes(StandardCharsets.UTF_8);
        System.out.println(bytes);
        ISerializer serializer = SerializerManager.getSerializer((byte) 1);

        DataFrame<Address> data = new DataFrame<>();
        Address address = new Address("localhost", 1212);
        data.setData(address);
        data.setClassname(Address.class.getName());


        byte[] srcData = serializer.serialize(data);

        DataFrame<?> subFrame = serializer.deserialize(srcData, DataFrame.class);

        String classname = subFrame.getClassname();
        Class<?> clazz = Class.forName(classname);
        byte[] serialize = serializer.serialize(subFrame.getData());
        Object deserialize = serializer.deserialize(serialize, clazz);
        System.out.println(deserialize);
    }
}
