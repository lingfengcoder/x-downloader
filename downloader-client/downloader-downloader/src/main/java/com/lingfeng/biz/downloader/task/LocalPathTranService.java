package com.lingfeng.biz.downloader.task;

import com.lingfeng.biz.downloader.config.FtpConfig;
import com.lingfeng.biz.downloader.config.StoreConfig;
import com.lingfeng.biz.downloader.model.FTPUrlInfo;
import com.lingfeng.biz.downloader.util.FTPUtils;
import com.lingfeng.biz.downloader.util.UrlParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.lingfeng.biz.downloader.util.UrlParser.*;

/**
 * @Author: wz
 * @Date: 2021/11/16 20:31
 * @Description: 本地路径转换服务
 * //通过存储id获取本地文件前缀
 * http://172.1.1.1:9090/a/b/c/index.m3u8 => /data/a/b/c/index.m3u8
 * ftp://vstore:pwd@127.0.0.1:8868/a/b/c/index.m3u8 =>/data/vstorage/a/b/c/index.m3u8
 */
@Component
public class LocalPathTranService {

    @Autowired
    private StoreConfig storeConfig;
    @Autowired
    private FtpConfig ftpConfig;

    // ftp://vstore:pwd@127.0.0.1:8868/a/b/c/index.m3u8 =>/data/vstorage/a/b/c/index.m3u8

    /**
     * @param url, storeId
     * @return java.lang.String
     * @Description 根据存储id将 目标路径转换为本地路径
     * @author wz
     * @date 2021/11/18 17:13
     */
    public String tranFileLocalPath(String url, long storeId) {
        String prefix = storeConfig.getPrefixById(storeId);
        String middlePath;
        url = url.trim();
        if (url.toLowerCase().startsWith(HTTP_PROTOCOL) || url.toLowerCase().startsWith(HTTPS_PROTOCOL)) {
            middlePath = httpRelativePath(url);
        } else if (url.startsWith(FTP_PROTOCOL)) {
            middlePath = ftpRelativePath(url);
        } else {
            throw new RuntimeException("不支持的协议:" + url);
        }
        return UrlParser.clearSlash(prefix + "/" + middlePath);
    }


    //获取ftp的相对文件路径
    //ftp://vstore:pwd@127.0.0.1:8868/a/b/c/index.m3u8 =>/vstorage/a/b/c/index.m3u8
    private String ftpRelativePath(String url) {
        FTPUrlInfo info = FTPUtils.parseFTPUrl(url);
        return ftpConfig.tranPathByUsername(info.getUserName()) + "/" + info.getFilePath();
    }

    //获取http的相对文件路径
    //http://172.1.1.1:9090/a/b/c/index.m3u8 => /a/b/c/index.m3u8
    private String httpRelativePath(String url) {
        return UrlParser.parseRelativePathWithFilename(url);
    }

}
