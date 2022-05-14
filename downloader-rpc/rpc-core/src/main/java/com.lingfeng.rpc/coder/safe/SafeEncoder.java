package com.lingfeng.rpc.coder.safe;

import com.lingfeng.rpc.frame.SafeFrame;
import com.lingfeng.rpc.serial.ISerializer;
import com.lingfeng.rpc.serial.SerializerManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

/**
 * @Author: wz
 * @Date: 2022/5/10 14:54
 * @Description: 自定义消息编码器
 */
public class SafeEncoder extends MessageToByteEncoder<SafeFrame<? extends Serializable>> {

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
    protected void encode(ChannelHandlerContext ctx, SafeFrame<? extends Serializable> safeFrame, ByteBuf out) throws Exception {
        //对内容进行序列化
        Serializable content = safeFrame.getContent();
        ISerializer serializer = SerializerManager.getSerializer(safeFrame.getSerial());
        byte[] data = serializer.serialize(content); //序列化

        safeFrame.setLength(data.length);
        out.writeByte(safeFrame.getCmd());//1
        out.writeByte(safeFrame.getSerial());//1
        out.writeByte(safeFrame.getEncrypt());//1
        out.writeLong(safeFrame.getTimestamp());//8
        out.writeLong(safeFrame.getClient());//8
        out.writeBytes(safeFrame.getSign().getBytes(StandardCharsets.UTF_8));//32
        //content clazz bytes 使用;进行分割
        byte[] clazzBytes = (content.getClass().getName() + ";").getBytes(StandardCharsets.UTF_8);
        //(content clazz len)  +(content  len)
        out.writeInt(clazzBytes.length + data.length);//4
        // content clazz
        out.writeBytes(clazzBytes);
        //content
        out.writeBytes(data);
    }
}
