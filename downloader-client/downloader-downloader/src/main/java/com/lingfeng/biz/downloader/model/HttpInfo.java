package com.lingfeng.biz.downloader.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @Author: wangbo
 * @Date: 2022/4/2 9:20
 */
@Getter
@Setter
@Accessors(chain = true)
public class HttpInfo {

    /**
     * http 全路径 http://127.0.0.1:8080/123/abc/index.m3u8
     */
    private String remoteUrl;
    /**
     * 本地文件全路径 /data/store/index.m3u8
     */
    private String localFilePath;
    /**
     * 本地路径前缀 /data/store/
     */
    private String localPrefix;
    /**
     * 文件code
     */
    private String fileCode;

}
