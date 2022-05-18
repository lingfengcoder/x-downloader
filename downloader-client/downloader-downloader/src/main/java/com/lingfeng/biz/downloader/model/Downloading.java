package com.lingfeng.biz.downloader.model;


import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import com.lingfeng.biz.downloader.util.TempFileUtil;
import com.lingfeng.biz.downloader.util.UrlParser;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * @Author: wz
 * @Date: 2021/11/29 21:29
 * @Description: 下载文件 所生成的临时记录文件
 */
@Getter
@Setter
@Slf4j
@ToString
@Accessors(chain = true)
@AllArgsConstructor
public class Downloading implements Serializable {
    //是否下载完毕
    private boolean finish;
    //文件路径
    private String filepath;
    //文件名
    private String filename;
    //文件类型
    private String fileType;
    //文件大小
    private Long fileSize;
    //文件切片个数
    private Integer sliceCount;


    public static void main(String[] args) {
        System.out.println(TmpIndexSystem.TIMEOUT);
    }
    //内置 临时下载文件 索引存储系统
    //每过6H清理一次临时下载文件和索引文件
    static class TmpIndexSystem {
        //索引文件存放目录
        private final static String TEMP_FILE_INDEX_PREFIX = "/data/download/tmp/index/";
        //文件超时时间
        private static long TIMEOUT = 1000 * 60 * 60 * 3;//6小时
        //文件索引时间目录
        private final static String TIME_FORMAT = "yyyyMMddHH";//yyyy-MM-dd HH:mm:ss
        //索引文件后缀
        private final static String FILE_INDEX_SUFFIX = ".index";

        //扫描并处理临时索引和文件
        public static void scanIndex(long now, long timeout) {
            if (timeout > 0) TIMEOUT = timeout;
            long outTime = now - TIMEOUT;
            File file = new File(TEMP_FILE_INDEX_PREFIX);
            if (file.exists()) {
                File[] files = file.listFiles();
                if (files != null) {
                    for (File dir : files) {
                        //dir = 2021120617
                        handleDir(dir, outTime);
                    }
                }
            }
        }

        //生成索引文件并写入数据 用于定时器删除
        public static void genIndex(String filePath, boolean finished) {
            try {
                Date date = new Date();
                long now = date.getTime();
                String tempFilePath = TempFileUtil.getTempFilePath(filePath);
                String data = generalData(tempFilePath, finished ? now : 0, !finished ? 0 : now);
                String filename = UrlParser.parseFileName(filePath);
                generalIndex(data, filename, date);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        //找到索引目录下所有的 时间目录，如果超时就进行目标文件和索引文件的删除
        private static void handleDir(File dir, long outTime) {
            try {
                //2021120617
                String name = dir.getName();
                log.info("【TMP INDEX】开始处理{}目录下的临时索引文件", dir.getAbsoluteFile());
                DateTime parse = DateUtil.parse(name, TIME_FORMAT);
                long time = parse.getTime();
                if (time <= outTime) {
                    //需要删除的文件夹
                    File[] files = dir.listFiles();
                    if (files != null) {
                        log.info("【TMP INDEX】{} 共有{}个文件", name, files.length);
                        for (File file : files) {
                            handleIndexFile(file);
                        }
                    } else {
                        if (FileUtil.isDirEmpty(dir)) {
                            dir.delete();
                        }
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            } finally {
                try {
                    //如果目录为空，删除目录
                    if (FileUtil.isDirEmpty(dir)) {
                        dir.delete();
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }

        //解析索引文件并删除目标文件和索引文件
        private static void handleIndexFile(File file) {
            try {
                //FilePath|finishTime|errorTime
                String data = FileUtil.readString(file, StandardCharsets.UTF_8);
                if (ObjectUtil.isNotEmpty(data)) {
                    String[] split = data.split("\\|");
                    String path = split[0];
                    log.info("【TMP INDEX】删除 临时下载文件 {}", path);
                    boolean del = FileUtil.del(path);
                    if (del) {
                        log.info("【TMP INDEX】删除 临时下载文件 的 索引文件 {}", path);
                        if (!file.delete()) {
                            log.error("删除 临时下载文件 的 索引文件 删除失败{}", file.getAbsoluteFile());
                        }
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        //生成索引文件
        private static String generalIndex(String data, String filename, Date now) {
            String filepath = null;
            try {
                // /data/download/tmp/index/2021120617/bbq.index
                filepath = TEMP_FILE_INDEX_PREFIX
                        + File.separator + DateUtil.format(now, TIME_FORMAT)
                        + File.separator + filename + FILE_INDEX_SUFFIX;
                File index = new File(filepath);
                FileUtil.mkParentDirs(index);
                index.createNewFile();
                FileUtil.writeString(data, index, StandardCharsets.UTF_8);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
            return filepath;
        }

        //生成索引数据
        private static String generalData(String filePath, long finish, long error) {
            return filePath + "|" + (finish == 0 ? "" : finish) + "|" + (error == 0 ? "" : error);
        }
    }

    public Downloading(String filepath) {
        try {
            this.filepath = filepath;
            this.filename = FileUtil.getName(filepath);
            this.fileType = FileUtil.getSuffix(filepath);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public Downloading(String filepath, int sliceCount) {
        try {
            this.filepath = filepath;
            this.sliceCount = sliceCount;
            this.filename = FileUtil.getName(filepath);
            this.fileType = FileUtil.getSuffix(filepath);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public Downloading(String filepath, long fileSize) {
        try {
            this.filepath = filepath;
            this.fileSize = fileSize;
            this.filename = FileUtil.getName(filepath);
            this.fileType = FileUtil.getSuffix(filepath);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public static void clearTmpIndex(long timeout) {
        log.info("开始清理下载文件索引");
        TmpIndexSystem.scanIndex(new Date().getTime(), timeout);
    }

    //生成下载的临时文件
    public static String generalTemp(String filePath, Downloading data) {
        String tmpFilePath = null;
        try {
            log.info("filePath:{} generalTemp:{} ", filePath, data);
            //step1:生成临时文件
            File file = TempFileUtil.generalTempFile(filePath);
            if (file == null || data == null) return null;
            //step2:写入数据
            FileUtil.writeString(JSONObject.toJSONString(data), file, StandardCharsets.UTF_8);
            tmpFilePath = file.getAbsolutePath();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } finally {
            log.info("临时文件路径:{}", tmpFilePath);
        }
        return tmpFilePath;
    }

    //从临时文件中获取文件信息
    public static Downloading readTemp(String filePath) {
        try {
            //获取临时文件
            File tmpFile = TempFileUtil.getTmpFile(filePath);
            if (tmpFile != null) {
                String data = FileUtil.readString(tmpFile, StandardCharsets.UTF_8);
                if (ObjectUtil.isNotEmpty(data)) {
                    log.info("readTemp: {}", data);
                    return JSONObject.parseObject(data, Downloading.class);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    //删除临时文件
    public static void delTemp(String filePath) {
        try {
            File tmpFile = TempFileUtil.getTmpFile(filePath);
            tmpFile.delete();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    //文件下载完毕后更新临时文件
    public static void updateTemp(String filePath, boolean finish, Downloading backup) {
        try {
            Downloading data = readTemp(filePath);
            if (data == null) {
                data = backup;
            }
            if (data != null) {
                data.setFinish(finish);
                generalTemp(filePath, data);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            //只有在下载完成或者下载失败后会生成索引，用于定时删除临时文件
            TmpIndexSystem.genIndex(filePath, finish);
        }
    }
}
