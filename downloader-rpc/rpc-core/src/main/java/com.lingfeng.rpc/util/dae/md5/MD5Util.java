package com.lingfeng.rpc.util.dae.md5;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class MD5Util {


    /**
     * 生成md5
     *
     * @param message
     * @return
     */
    public static String md5_32(String message) {
        StringBuilder md5str = new StringBuilder();
        try {
            // 1 创建一个提供信息摘要算法的对象，初始化为md5算法对象
            MessageDigest md = MessageDigest.getInstance("MD5");

            // 2 将消息变成byte数组
            byte[] input = message.getBytes(StandardCharsets.UTF_8);

            // 3 计算后获得字节数组,这就是那128位了
            byte[] buff = md.digest(input);

            // 4 把数组每一字节（一个字节占八位）换成16进制连成md5字符串
            md5str.append(bytesToHex(buff));

        } catch (Exception e) {
            e.printStackTrace();
        }
        return md5str.toString();
    }

    /**
     * 二进制转十六进制
     *
     * @param bytes
     * @return
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder md5str = new StringBuilder();
        // 把数组每一字节换成16进制连成md5字符串
        int digital;
        for (int i = 0; i < bytes.length; i++) {
            digital = bytes[i];

            if (digital < 0) {
                digital += 256;
            }
            if (digital < 16) {
                md5str.append("0");
            }
            md5str.append(Integer.toHexString(digital));
        }
        return md5str.toString().toUpperCase();
    }


    public static void main(String[] args) {
        String str = "0123456789";

        String s = md5_32(str);
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        System.out.println(bytes.length);
        // System.out.println(DigestUtils.md5DigestAsHex(str.getBytes(StandardCharsets.UTF_8)));
    }


}
