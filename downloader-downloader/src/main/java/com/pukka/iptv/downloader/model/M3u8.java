package com.pukka.iptv.downloader.model;

import cn.hutool.core.util.ObjectUtil;
import com.pukka.iptv.downloader.util.UrlParser;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * @Author: wz
 * @Date: 2021/10/26 20:09
 * @Description: m3u8封装对象
 * 原始索引地址 url = http://127.0.0.1:8080/a/b/c/index.m3u8
 * 本地文件前缀 localPrefix = /data/m3u8/xxx/
 * 原始索引地址 前缀 httpPrefix = http://127.0.0.1:8080/a/b/c/
 * 从index.m3u8读取的index2文件相对坐标 line = m/n/index2.m3u8
 * index2文件绝对坐标 httpPrefix+line = http://127.0.0.1:8080/a/b/c/m/n/index2.m3u8
 * 本地文件需要剔除的前缀:/a/b/c/
 * index本地文件路  /data/m3u8/xxx/index.m3u8
 * index2本地文件路径 /data/m3u8/xxx/m/n/index2.m3u8
 */
@Setter
@Getter
@Accessors(chain = true)
@ToString
public class M3u8 {
    public final static String M3U8_FLAG = "M3U8";
    //http://127.0.0.1:8080/123/abc
    private String remoteUrlPrefix;
    //本地路径前缀 /data/store/
    private String localPrefix;
    //http 全路径 http://127.0.0.1:8080/123/abc/index.m3u8
    private String remoteUrl;
    //目标存储 FTP 全路径
    private String targetUrl;
    //本地文件全路径 /data/store/index.m3u8
    private String localFilePath;
    //需要删除的前缀 /123/abc
    private String delPrefix;
    //视频时常
    private Float duration = 0F;
    private long fileSize = 0L;
    //所有文件都下载完毕
    private boolean allDown;


    //计算文件时长
    public float addDuration(float t) {
        return this.duration += t;
    }

    //计算文件大小
    public float addFileSize(long s) {
        return this.fileSize += s;
    }

    //其中的一片TS
    private Ts slice;
    //包含的个数
    private int tsTotal;
    //ts下载完成个数
    private int tsFinishCount;

    //增加ts切片个数
    public int addTsCount() {
        return ++this.tsTotal;
    }

    public int addTsCount(int count) {
        return this.tsTotal += count;
    }

    //增加ts完成个数
    public int addTsFinishCount() {
        return ++this.tsFinishCount;
    }

    public int addTsFinishCount(int count) {
        return this.tsFinishCount += count;
    }

    //构造私有化
    private M3u8() {
    }

    // 原始索引地址 url = http://127.0.0.1:8080/a/b/c/index.m3u8
    // 本地文件前缀 localPrefix = /data/m3u8/xxx/
    // 原始索引地址 前缀 httpPrefix = http://127.0.0.1:8080/a/b/c/
    // 从index.m3u8读取的index2文件相对坐标 line = m/n/index2.m3u8
    // index2文件绝对坐标 httpPrefix+line = http://127.0.0.1:8080/a/b/c/m/n/index2.m3u8
    // 本地文件需要剔除的前缀:/a/b/c/
    // index本地文件路  /data/m3u8/xxx/index.m3u8
    // index2本地文件路径 /data/m3u8/xxx/m/n/index2.m3u8

    /**
     * @param url           http://127.0.0.1:8080/abc/123/index.m3u8
     * @param localFilePath /data/a/b/c/tmp.m3u8
     * @Description: 根据m3u8地址和本地存储前缀生成m3u8（自动剔除中间路径）
     * @return: com.pukka.iptv.downloader.model.M3u8
     * @author: wz
     * @date: 2021/11/15 10:17
     */
    public static M3u8 generalM3u8(String url, String localFilePath, String targetUrl) {
        String middlePrefix = UrlParser.parseRelativePath(url); // /abc/123/
        String localPrefix = UrlParser.parseRelativePath(localFilePath); // /data/a/b/c
        return generalInnerM3u8(url, middlePrefix, localPrefix, targetUrl);
    }


    /**
     * @Description: 根据m3u8地址和本地存储前缀生成m3u8（需要手动指定要剔除的中间路径）
     * @param: url:源地址 http://172.1.1.1:9090/zz/yy/xx/index.m3u8?x=1&y=2
     * middlePrefix:相对路径   /zz/yy/xx/
     * localPrefix: 本地路径前缀 /data/m3u8/xxx/
     * 本地完整路径 /data/m3u8/xxx/index.m3u8
     */
    public static M3u8 generalInnerM3u8(String url, String middlePrefix, String localPrefix, String targetUrl) {
        String middleUrl = UrlParser.clearPrefixSuffix(url);// /zz/yy/xx/index.m3u8
        if (!ObjectUtil.isEmpty(middlePrefix) && middleUrl.startsWith(middlePrefix)) {
            //踢出 index.m3u8 原连接中间的 根部相对路径 保留后面的文件存储路径
            middleUrl = middleUrl.substring(middlePrefix.length());
        }
        return new M3u8().setRemoteUrl(url)//设置完整http url http://127.0.0.1:8080/abc/123/5.ts?a=1&b=2
                .setRemoteUrlPrefix(UrlParser.parsePrefix(url))//设置http的前缀 http://127.0.0.1:8080/abc/123/
                .setLocalFilePath(localPrefix + middleUrl)//本地文件路径 /data/store/m3u8/abc/123/5.ts
                .setLocalPrefix(localPrefix)//本地文件前缀 /data/store/m3u8/
                .setDelPrefix(middlePrefix)
                .setTargetUrl(targetUrl)
                .setAllDown(true);//初始化认为全部文件可以下载
    }

    public static boolean isM3u8Url(String url) {
        return M3U8_FLAG.equalsIgnoreCase(UrlParser.parseFileSuffix(url));
    }

    @Setter
    @Getter
    @Accessors(chain = true)
    public static class Ts {
        private String httpUrl;
        private String localPath;
        private boolean down = false;//下载完毕

        //构造私有化
        private Ts() {
        }

        //生成ts文件
        public static Ts generalTsFile(String url, String delPrefix, String localPrefix) {
            String middleUrl = UrlParser.clearPrefixSuffix(url);// /abc/123/5.ts
            if (!ObjectUtil.isEmpty(delPrefix) && middleUrl.startsWith(delPrefix)) {
                middleUrl = middleUrl.substring(delPrefix.length());
            }
            return new Ts().setHttpUrl(url).setLocalPath(localPrefix + middleUrl);
        }
    }
}
