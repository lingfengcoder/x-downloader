package com.pukka.iptv.downloader.util;

import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;

/**
 * @Author: wz
 * @Date: 2021/11/29 21:41
 * @Description:
 */
@Slf4j
public class IoUtil {

    public static void closeStream(Closeable... io) {
        if (io == null) return;
        for (Closeable closeable : io) {
            try {
                if (closeable != null) closeable.close();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }
}
