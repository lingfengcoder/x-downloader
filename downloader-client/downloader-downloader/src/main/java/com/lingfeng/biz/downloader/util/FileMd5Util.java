package com.lingfeng.biz.downloader.util;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.nacos.common.utils.MD5Utils;
import lombok.extern.slf4j.Slf4j;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.nio.charset.StandardCharsets;
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


    public static String computeMD52(File... files) {
        DigestInputStream din = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            FileInputStream input = null;
            byte[] digest = null;
            for (File file : files) {
                input = new FileInputStream(file);
                BufferedInputStream bufferedInput = new BufferedInputStream(input);
                din = new DigestInputStream(bufferedInput, md5);
                byte[] b = new byte[1024];
                //read 方法中会根据数据的字节数据动态计算MD5 最终得到的结果和直接计算是一致的
                while (din.read(b) != -1) ;
                IoUtil.closeStream(din, bufferedInput, input);
            }
            digest = md5.digest();
            //第一个参数是一个输入流
            return DatatypeConverter.printHexBinary(digest);
        } catch (NoSuchAlgorithmException | IOException e) {
            IoUtil.closeStream(din);
            log.error(e.getMessage(), e);
        } finally {
            IoUtil.closeStream(din);
        }
        return null;
    }

    public static String computeMD5(String... data) {
        DigestInputStream din = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            for (String item : data) {
                byte[] bytes = item.getBytes(StandardCharsets.UTF_8);
                ByteArrayInputStream input = new ByteArrayInputStream(bytes);
                din = new DigestInputStream(input, md5);
                byte[] buffer = new byte[8848];
                while (din.read(buffer) != -1) {
                }
                IoUtil.closeStream(din, input);
            }
            byte[] digest = null;
            digest = md5.digest();
            return DatatypeConverter.printHexBinary(digest);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            IoUtil.closeStream(din);
        }
        return null;
    }

    public static void main(String[] args) {
        String path = "D:\\data\\slice\\slice\\";
        File dir = new File(path);
        File[] files = dir.listFiles();
        String s = computeMD52(files);
        log.info("子文件聚合MD5: {}", s);


        File srcFile = new File("D:\\data\\slice\\1.ts");
        String srcMD5 = computeMD52(srcFile);
        log.info("源文件MD5: {}", srcMD5);
    }


}
