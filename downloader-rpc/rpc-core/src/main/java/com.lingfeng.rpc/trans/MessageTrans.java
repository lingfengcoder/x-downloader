package com.lingfeng.rpc.trans;

import com.lingfeng.rpc.coder.safe.DataFrame;
import com.lingfeng.rpc.constant.EncryptType;
import com.lingfeng.rpc.constant.Cmd;
import com.lingfeng.rpc.constant.SerialType;
import com.lingfeng.rpc.frame.SafeFrame;


import com.lingfeng.rpc.sign.Signature;
import com.lingfeng.rpc.util.SystemClock;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

/**
 * @Author: wz
 * @Date: 2022/5/9 20:17
 * @Description:
 */
@Slf4j
public class MessageTrans {
    //默认加密方式
    private final static byte DEFAULT_ENCRYPT = EncryptType.NONE.code();
    //默认采用java序列化
    private final static byte DEFAULT_SERIAL = SerialType.JAVA_SERIAL.code();
    private final static String HEARTBEAT_CONTENT = "heart-beat";

    //心跳帧
    public static SafeFrame<String> heartbeatFrame(long clientId) {
        SafeFrame<String> safeFrame = new SafeFrame<String>()
                //心跳
                .setCmd(Cmd.HEARTBEAT.code())
                //序列化方式
                .setSerial(SerialType.STRING_SERIAL.code())
                //加密方式
                .setEncrypt(DEFAULT_ENCRYPT)
                //时间戳
                .setTimestamp(SystemClock.now())
                //客户端id
                .setClient(clientId)
                //内容
                .setContent(HEARTBEAT_CONTENT);
        //签名
        safeFrame.setSign(Signature.sign(safeFrame));
        return safeFrame;
    }


    //数据帧
    public static <T extends Serializable> SafeFrame<T> dataFrame(T data, Cmd cmd, long clientId) {
        SafeFrame<T> safeFrame = new SafeFrame<T>()
                //心跳
                .setCmd(cmd.code())
                //序列化方式
                .setSerial(SerialType.JAVA_SERIAL.code())
                //加密方式
                .setEncrypt(DEFAULT_ENCRYPT)
                //时间戳
                .setTimestamp(SystemClock.now())
                //客户端id
                .setClient(clientId);
        safeFrame.setContent(data);
        //签名
        safeFrame.setSign(Signature.sign(safeFrame));
        return safeFrame;
    }


    //认证帧
    public static SafeFrame<String> authFrame(String pwd, long clientId) {
        SafeFrame<String> safeFrame = new SafeFrame<String>()
                //心跳
                .setCmd(Cmd.AUTH.code())
                //序列化方式
                .setSerial(SerialType.STRING_SERIAL.code())
                //加密方式
                .setEncrypt(DEFAULT_ENCRYPT)
                //时间戳
                .setTimestamp(SystemClock.now())
                //客户端id
                .setClient(clientId)
                //密码
                .setContent(pwd);
        //签名
        safeFrame.setSign(Signature.sign(safeFrame));
        return safeFrame;
    }
}
