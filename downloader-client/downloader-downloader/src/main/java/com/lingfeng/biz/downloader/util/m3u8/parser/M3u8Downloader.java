package com.lingfeng.biz.downloader.util.m3u8.parser;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 * @Author: wz
 * @Date: 2021/11/13 20:31
 * @Description: m3u8到本地文件的下载
 */
interface M3u8Downloader {

    boolean downloadFile(String url, File file) throws Exception;

    default void closeStream(Closeable... closeable) {
        for (Closeable io : closeable) {
            try {
                if (io != null)
                    io.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
