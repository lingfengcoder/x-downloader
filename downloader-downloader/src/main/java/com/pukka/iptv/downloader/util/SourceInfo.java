package com.pukka.iptv.downloader.util;

import com.pukka.iptv.downloader.model.Downloading;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: wz
 * @Date: 2021/12/8 11:50
 * @Description:
 */

@FunctionalInterface
public interface SourceInfo {
    void info(Downloading info);

    static void call(SourceInfo func, Downloading info) {
        try {
            if (func != null)
                func.info(info);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
