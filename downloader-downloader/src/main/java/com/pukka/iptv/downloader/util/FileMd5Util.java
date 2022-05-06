package com.pukka.iptv.downloader.util;

import lombok.extern.slf4j.Slf4j;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @Author: wz
 * @Date: 2021/10/27 21:51
 * @Description:计算文件的MD5值
 */
@Slf4j
public class FileMd5Util {

    public static String computeMD5(File file) {
        try {
            if (file != null && file.exists()) {
                return computeMD5(new FileInputStream(file));
            }
        } catch (FileNotFoundException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public static String computeMD5(FileInputStream input) {
        DigestInputStream din = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            //第一个参数是一个输入流
            din = new DigestInputStream(new BufferedInputStream(input), md5);
            byte[] b = new byte[1024];
            while (din.read(b) != -1) ;
            byte[] digest = md5.digest();
            return DatatypeConverter.printHexBinary(digest);
        } catch (NoSuchAlgorithmException | IOException e) {
            log.error(e.getMessage(), e);
        } finally {
            IoUtil.closeStream(din, input);
        }
        return null;
    }


}
