package com.lingfeng.biz.downloader.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Objects;

/**
 * @Author jxm
 * @Date 2021-10-14 15:02
 * @Description FTP文件地址解析信息
 */
@Getter
@Setter
@Accessors(chain = true)
@ToString
// 例子：ftp://username:pwd@127.0.0.1:8080/abc/123/1.mp4
public class FTPUrlInfo {
    // ip地址 127.0.0.1
    private String ip;
    // 端口 8080
    private int port;
    // 用户名 username
    private String userName;
    // 密码 pwd
    private String password;
    // 路径 /abc/123/
    private String path;
    // 文件名  1.mp4
    private String fileName;
    // 文件定位路径 /abc/123/1.mp4
    private String filePath;
    // 文件格式 mp4
    private String format;


}
