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

import java.nio.charset.StandardCharsets;
import java.util.List;

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
            int i = in.readableBytes();
            //此处可读的应该会大于32 可能连接着后面的数据
            log.info("readableBytes={}", i);
            in.readBytes(signByte, 0, 32);
            String sign = new String(signByte, StandardCharsets.UTF_8);//32

            //读取length字段
            int length = in.readInt();//4
            if (in.readableBytes() != length) {
                throw new RuntimeException("长度与标记不符");
            }
            //读取body
            byte[] bytes = new byte[in.readableBytes()];
            in.readBytes(bytes);

            //.content(new String(bytes, StandardCharsets.UTF_8)).build();
            //根绝数据帧获取序列化工具
            ISerializer serializer = SerializerManager.getSerializer(serial);

            if (SerialType.STRING_SERIAL.code() == serial) {
                SafeFrame<String> safeFrame = new SafeFrame<>();
                String data = serializer.deserialize(bytes, String.class);
                safeFrame.setContent(data);
                safeFrame.setCmd(cmd)
                        .setSerial(serial)
                        .setEncrypt(encrypt)
                        .setTimestamp(timestamp)
                        .setClient(clientId)
                        .setSign(sign)
                        .setLength(length);
                out.add(safeFrame);
            } else {
                SafeFrame<SubReqFrame<?>> safeFrame = new SafeFrame<>();
                SubReqFrame<?> subFrame = serializer.deserialize(bytes, SubReqFrame.class);
                safeFrame.setContent(subFrame);
                safeFrame.setCmd(cmd)
                        .setSerial(serial)
                        .setEncrypt(encrypt)
                        .setTimestamp(timestamp)
                        .setClient(clientId)
                        .setSign(sign)
                        .setLength(length);
                out.add(safeFrame);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public static void main(String[] args) throws ClassNotFoundException {
        ISerializer serializer = SerializerManager.getSerializer((byte) 1);

        SubReqFrame<Address> data = new SubReqFrame<>();
        Address address = new Address("localhost", 1212);
        data.setData(address);
        data.setClassname(Address.class.getName());


        byte[] srcData = serializer.serialize(data);

        SubReqFrame<?> subFrame = serializer.deserialize(srcData, SubReqFrame.class);

        String classname = subFrame.getClassname();
        Class<?> clazz = Class.forName(classname);
        byte[] serialize = serializer.serialize(subFrame.getData());
        Object deserialize = serializer.deserialize(serialize, clazz);
        System.out.println(deserialize);
    }
}
