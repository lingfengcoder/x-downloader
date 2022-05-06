package com.pukka.iptv.downloader.util.m3u8.parser;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import com.pukka.iptv.downloader.model.ConnectHead;
import com.pukka.iptv.downloader.model.M3u8;
import com.pukka.iptv.downloader.util.IoUtil;
import com.pukka.iptv.downloader.util.SourceInfo;
import com.pukka.iptv.downloader.util.UrlParser;
import com.pukka.iptv.downloader.util.m3u8.M3u8Util;
import com.pukka.iptv.downloader.util.m3u8.api.M3u8Api;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @Author: wz
 * @Date: 2021/11/13 19:14
 * @Description:
 */
@Slf4j
public abstract class M3u8Parser implements M3u8Api {
    enum Action {
        DEL,//删除
        DOWNLOAD,//下载
        COLLECT//统计ts个数
    }

    //m3u8文件个性标识
    private final static String M3U8_SUFFIX = ".m3u8";
    private final static String TS_SUFFIX = ".ts";
    private final static String TS_START_FLAG = "#EXTINF:";
    private final static String TS_FILE_SIZE_FLAG = "FILESIZE=";
    //批量下载的数据
    private final static int BATCH_SIZE = 10;
    //下载协议（方式）的threadLocal
    private static final ThreadLocal<ConnectHead> headThreadLocal = new ThreadLocal<>();

    //获取当前头部信息
    public static ConnectHead getCurrentHead() {
        return headThreadLocal.get();
    }

    //前置处理器
    private static void preProcess(Supplier<ConnectHead> supplier) {
        headThreadLocal.set(supplier.get());
    }

    //后置处理器
    private void postProcess(Consumer<ConnectHead> consumer) {
        try {
            if (consumer != null)
                consumer.accept(headThreadLocal.get());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            headThreadLocal.remove();
        }
    }

    protected M3u8 download(M3u8 m3u8, Supplier<ConnectHead> preFunc, Consumer<ConnectHead> endFunc) {
        try {
            preProcess(preFunc);
            return loopDownload(m3u8, null);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            postProcess(endFunc);
        }
        return null;
    }

    //递归下载m3u8
    protected static M3u8 loopDownload(M3u8 m3u8, SourceInfo info) {
        String url = m3u8.getRemoteUrl();
        //初始化全部下载完毕标识
        m3u8.setAllDown(true);
        //下载m3u8
        download(url, m3u8.getLocalFilePath());
        //设置临时下载文件信息
        //todo 如果要使用临时文件存储M3U8文件信息　
        // 可以采用多线程下载TS并 优先把所有的索引文件都下载完毕　然后解析索引文件统计个数
        //SourceInfo.call(info, new Downloading(m3u8.getLocalFilePath()));
        //解析并下载 ts 并返回 文件内的m3u8文件
        List<M3u8> m3U8s;
        try {
            m3U8s = analysisWithAction(m3u8, M3u8HttpParser.Action.DOWNLOAD);
        } catch (FileNotFoundException e) {
            log.error(e.getMessage());
            return null;//文件不存在返回null
        }
        if (!ObjectUtil.isEmpty(m3U8s)) {
            for (M3u8 item : m3U8s) {
                //递归下载
                M3u8 tmp = loopDownload(item, null);
                if (tmp != null) {
                    if (!tmp.isAllDown()) {
                        m3u8.setAllDown(false);
                    }
                    //设置视频的总时长
                    m3u8.addDuration(tmp.getDuration());
                    //设置视频的大小
                    m3u8.addFileSize(tmp.getFileSize());
                }
            }
        }
        return m3u8;
    }

    /**
     * @Description: m3u8文件的删除 递归删除所有的索引和ts
     * @param: [url, localPrefix]
     * @return: boolean
     * @author: wz
     * @date: 2021/10/29 20:54
     */
    public boolean deleteM3u8(M3u8 m3U8) {
        boolean allDel = true;
        List<M3u8> m3U8s;
        try {
            //解析并删除 ts 并返回 文件内的m3u8文件
            m3U8s = analysisWithAction(m3U8, M3u8HttpParser.Action.DEL);
        } catch (FileNotFoundException e) {
            log.error("文件不存在" + e.getMessage(), e);
            return false;
        }
        if (!ObjectUtil.isEmpty(m3U8s)) {
            for (M3u8 item : m3U8s) {
                //递归删除index
                boolean b = deleteM3u8(item);
                if (!b) {
                    allDel = false;
                }
            }
        }
        //m3u8所在目录如果为空，则删除
        deleteM3u8Index(m3U8);
        return allDel;
    }

    /**
     * @Description: 获取m3u8文件已经下载完成的个数
     * @return: com.pukka.iptv.downloader.model.M3u8
     * @author: wz
     * @date: 2021/10/29 20:55
     */
    public M3u8 getM3u8FinishCount(M3u8 m3U8) {
        //解析并下载 ts 并返回 文件内的m3u8文件
        List<M3u8> m3U8s;
        try {
            m3U8s = analysisWithAction(m3U8, M3u8HttpParser.Action.COLLECT);
        } catch (FileNotFoundException e) {
            log.error("文件不存在 " + e.getMessage(), e);
            return null;
        }
        if (!ObjectUtil.isEmpty(m3U8s)) {
            for (M3u8 item : m3U8s) {
                //递归计算
                M3u8 tmp = getM3u8FinishCount(item);
                if (tmp != null) {
                    //ts总数
                    m3U8.addTsCount(tmp.getTsTotal());
                    //下载完成数
                    m3U8.addTsFinishCount(tmp.getTsFinishCount());
                }
            }
        }
        return m3U8;
    }

    //对索引文件原本的信息进行统计
    private static void m3u8infoAnalysis(M3u8 m3U8, String line) {
        //文件大小的解析 (采用本地文件獲取大小)
        // m3U8.addFileSize(getTsFileSize(line));
        //时长的解析
        m3U8.addDuration(getTsDuration(line));
    }

    //收集内部的索引
    private static void collectInnerIndex(String line, M3u8 m3U8, List<M3u8> m3u8List) {
        String httpPrefix = m3U8.getRemoteUrlPrefix();
        String localPrefix = m3U8.getLocalPrefix();
        // localPrefix=/data/m3u8/xxx/
        //url = http://127.0.0.1:8080/a/b/c/index.m3u8
        //httpPrefix = http://127.0.0.1:8080/a/b/c/
        // line = m/n/index2.m3u8
        // httpPrefix + line =http://127.0.0.1:8080/a/b/c/m/n/index2.m3u8
        // 本地文件需要剔除的前缀:/a/b/c/
        //本地文件路  /data/m3u8/xxx/index.m3u8
        //本地文件路径 /data/m3u8/xxx/m/n/index2.m3u8
        M3u8 m3u8 = M3u8.generalInnerM3u8(httpPrefix + line, m3U8.getDelPrefix(), localPrefix, m3U8.getTargetUrl());
        m3u8List.add(m3u8);
    }

    //收集ts
    private static void collectTs(String line, M3u8 m3U8, List<M3u8.Ts> tsList) {
        String httpPrefix = m3U8.getRemoteUrlPrefix();
        String localPrefix = m3U8.getLocalPrefix();
        if (line.startsWith(UrlParser.HTTP_PROTOCOL) || line.startsWith(UrlParser.HTTPS_PROTOCOL)) {
            //暂不处理 全连接的
            tsList.add(M3u8.Ts.generalTsFile(line, m3U8.getDelPrefix(), localPrefix));
        } else {
            tsList.add(M3u8.Ts.generalTsFile(httpPrefix + line, m3U8.getDelPrefix(), localPrefix));
        }
    }


    //解析并下载 ts 并返回 文件内的m3u8文件
    private static List<M3u8> analysisWithAction(M3u8 m3U8, M3u8HttpParser.Action action) throws FileNotFoundException {
        List<M3u8.Ts> tsList = new ArrayList<>();
        List<M3u8> m3u8List = new ArrayList<>();
        RandomAccessFile file = null;
        if (!FileUtil.exist(m3U8.getLocalFilePath())) {
            return m3u8List;
        }
        try {
            file = new RandomAccessFile(m3U8.getLocalFilePath(), "rw");
            //循环读取行
            boolean hasNext;
            do {
                hasNext = analysisLine(m3U8, file, m3u8List, tsList);
                //ts满足一批 进行下载
                if (tsList.size() >= BATCH_SIZE) {
                    tsAction(m3U8, tsList, action);
                }
            } while (hasNext);
        } finally {
            IoUtil.closeStream(file);
        }
        //收尾工作 剩余不足一批的也要操作
        if (!ObjectUtil.isEmpty(tsList)) {
            tsAction(m3U8, tsList, action);
        }
        tsList = null;
        return m3u8List;
    }

    private static void tsAction(M3u8 m3U8, List<M3u8.Ts> tsList, M3u8HttpParser.Action action) {
        try {
            switch (action) {
                //下载ts
                case DOWNLOAD:
                    downloadTs(m3U8, tsList);
                    break;
                //删除ts
                case DEL:
                    deleteTs(m3U8, tsList);
                    //如果ts都清空了删除目录
                    delTsDir(tsList);
                    break;
                //统计完成个数
                case COLLECT:
                    collectTsFinishCount(m3U8, tsList);
                    break;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            tsList.clear();
        }
    }

    //解析一行的数据
    private static boolean analysisLine(M3u8 m3U8, RandomAccessFile file, List<M3u8> m3u8List, List<M3u8.Ts> tsList) {
        String line = FileUtil.readLine(file, StandardCharsets.UTF_8);
        if (line == null) return false;//读取到尾部
        line = UrlParser.clearLine(line);
        //对带#数据的处理
        if (line.startsWith("#")) {
            m3u8infoAnalysis(m3U8, line);
        } else {
            //解析文件名称
            String filename = UrlParser.parseFileName(line);
            //m3u8文件
            if (filename.endsWith(M3U8_SUFFIX)) {
                collectInnerIndex(line, m3U8, m3u8List);
            }
            //ts解析
            if (filename.endsWith(TS_SUFFIX)) {
                //ts文件数量+1
                m3U8.addTsCount();
                collectTs(line, m3U8, tsList);
            }
        }
        return true;
    }


    //获取ts切片的文件大小
    private static long getTsFileSize(String line) {
        try {
            line = line.toUpperCase();
            if (line.startsWith(TS_FILE_SIZE_FLAG)) {
                line = line.substring(line.indexOf(TS_FILE_SIZE_FLAG));
                if (line.contains(",")) {
                    line = line.substring(0, line.indexOf(","));
                }
                line = line.split("=")[1];
                return Long.parseLong(line);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return 0;
    }

    //获取视频时长
    private static float getTsDuration(String line) {
        // 格式化时间
        try {
            line = line.toUpperCase();
            if (line.startsWith(TS_START_FLAG)) {
                line = line.substring(TS_START_FLAG.length());
                if (line.contains(",")) {
                    line = line.substring(0, line.indexOf(","));
                }

                return Float.parseFloat(line);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return 0;
    }

    //统计ts完成个数
    private static void collectTsFinishCount(M3u8 m3U8, List<M3u8.Ts> tsList) {
        for (M3u8.Ts ts : tsList) {
            try {
                boolean exist = FileUtil.exist(ts.getLocalPath());
                if (exist) {
                    m3U8.addTsFinishCount();
                    ts.setDown(true);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    //删除TS
    private static void deleteTs(M3u8 m3U8, List<M3u8.Ts> tsList) {
        if (!ObjectUtil.isEmpty(tsList)) {
            for (M3u8.Ts ts : tsList) {
                String localPath = ts.getLocalPath();
                try {
                    FileUtil.del(localPath);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    //删除m3u8 索引文件
    private static void deleteM3u8Index(M3u8 m3U8) {
        String localPath = m3U8.getLocalFilePath();
        FileUtil.del(localPath);
        //如果目录为空就删除目录
        delDirIfEmpty(localPath);
    }

    private static void delDirIfEmpty(String dirPath) {
        if (ObjectUtil.isEmpty(dirPath)) return;
        try {
            File tmp = new File(dirPath);
            File parent = tmp.getParentFile();
            if (parent.exists()) {
                if (FileUtil.isDirEmpty(parent)) {
                    parent.delete();
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    //如果当前文件夹已经空，删除当前文件夹
    private static void delTsDir(List<M3u8.Ts> tsList) {
        for (M3u8.Ts ts : tsList) {
            delDirIfEmpty(ts.getLocalPath());
        }
    }

    //批量下载ts
    private static void downloadTs(M3u8 m3U8, List<M3u8.Ts> tsList) {
        if (!ObjectUtil.isEmpty(tsList)) {
            for (M3u8.Ts ts : tsList) {
                if (!download(ts.getHttpUrl(), ts.getLocalPath())) {
                    m3U8.setAllDown(false);
                } else {//下载成功
                    //追加文件大小
                    m3U8.addFileSize(getFileSize(ts.getLocalPath()));
                    //ts文件数加一
                    m3U8.addTsFinishCount();
                    //设置其中一片ts的记录
                    if (m3U8.getSlice() == null) {
                        m3U8.setSlice(ts);
                    }
                }
            }
        }
    }

    //获取文件大小
    private static long getFileSize(String path) {
        if (ObjectUtil.isEmpty(path)) {
            return 0L;
        }
        File file = new File(path);
        if (file.exists()) {
            return file.length();
        }
        return 0L;
    }

    public static File makeLocalFile(String path) throws IOException {
        File file = new File(path);
        //文件存在不下载
        if (FileUtil.exist(file)) {
            log.info("文件已经存在，不进行下载! {}", file.getAbsoluteFile());
            return null;
        }
        //创建父级目录
        FileUtil.mkParentDirs(file.getAbsolutePath());
        if (!file.createNewFile()) {
            log.error("文件创建失败！{}", file.getAbsoluteFile());
            return null;
        }
        return file;
    }

    /**
     * @Description: 下载文件到本地，文件存在不会下载，如果下载失败会删除临时文件
     * @param: [url, local]
     * @return: boolean
     * @author: wz
     * @date: 2021/10/26 21:16
     */
    private static boolean download(String url, String local) {
        File file = null;
        try {
            file = makeLocalFile(local);
            //不下载默认有效
            if (file == null) return true;
            return M3u8AbsDownloader.download(url, file);
        } catch (Exception e) {
            log.error("下载失败！url:{} local:{} exce:{}", url, local, e.getMessage(), e);
            //注意要关闭流 如果 http.execute 异常或者没有返回值，out是不执行的也就没有关闭
            delete(file);//删除文件
            //m3u8所在目录如果为空，则删除
            delDirIfEmpty(local);
            return false;
        }
    }

    //删除文件
    private static void delete(File file) {
        if (file != null && file.exists() && file.isFile()) {
            log.info("文件{}删除{}", file.getAbsoluteFile(), file.delete() ? "成功" : "失败!");
        }
    }


    public static void main(String[] args) throws Exception {
        String path = "/data/m3u8/m3u8demo/111.m3u8";
        M3u8 m3u8 = M3u8.generalM3u8("http://127.0.0.1:/a/b/c/111.m3u8", path, "http://127.0.0.1/a/b/c.m3u8");
        M3u8 count = M3u8Util.getInstance().getM3u8FinishCount(m3u8);
        log.info(count.toString());
        download("http://img.netbian.com/file/2019/0824/small4a68818befe4ec269bf52b6f6f6ccae61566660671.jpg","/data/m3u8/m3u8demo/small4a68818befe4ec269bf52b6f6f6ccae61566660671.jpg");
    }
}
