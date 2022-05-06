package com.pukka.iptv.downloader.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.ObjectUtil;
import com.pukka.iptv.downloader.model.Downloading;

import java.io.File;
import java.io.IOException;

/**
 * @Author: wz
 * @Date: 2021/11/29 20:56
 * @Description: 临时文件工具
 */
public class TempFileUtil {

    private final static String FILE_SUFFIX = ".downloading";

    public static String getTempFilePath(String filePath) {
        return filePath + FILE_SUFFIX;
    }

    //生成临时文件
    public static File generalTempFile(String filePath) throws IOException {
        if (ObjectUtil.isNotEmpty(filePath)) {
            filePath = filePath.trim();
            boolean hasSuffix = filePath.contains(".");
            if (!hasSuffix) {
                throw new RuntimeException("输入的并非一个文件路径");
            }
            File tmp = new File(getTempFilePath(filePath));
            if (!tmp.exists()) {
                FileUtil.mkParentDirs(tmp);
                return tmp.createNewFile() ? tmp : null;
            }
            return tmp;
        }
        return null;
    }


    //获取文件的临时文件
    public static File getTmpFile(String filePath) {
        if (ObjectUtil.isNotEmpty(filePath)) {
            File tmp = new File(getTempFilePath(filePath));
            return tmp.exists() ? tmp : null;
        }
        return null;
    }


}
