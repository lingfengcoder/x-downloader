package com.lingfeng.biz.downloader.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.net.ftp.FTPClient;


/**
 * @Author: wz
 * @Date: 2021/11/13 18:12
 * @Description:
 */
@Getter
@Setter
@Accessors(fluent = true)
public class ConnectHead {
    //连接协议
    private Protocol protocol;
    //连接代理
    private Proxy proxy;
    //源文件ftp客户端
    private FTPClient ftpClient;

    public static ConnectHead general(Protocol p) {
        return new ConnectHead().protocol(p);
    }
}
