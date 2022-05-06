package com.pukka.iptv.downloader.util;

import cn.hutool.core.util.ObjectUtil;
import com.pukka.iptv.downloader.model.Protocol;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: wz
 * @Date: 2021/11/13 21:23
 * @Description: URL地址解析器
 */
@Slf4j
public class UrlParser {

    public final static String FTP_PROTOCOL = "ftp://";
    public final static String HTTP_PROTOCOL = "http://";
    public final static String HTTPS_PROTOCOL = "https://";
    public final static String[] PROTOCOLS = {FTP_PROTOCOL, HTTP_PROTOCOL, HTTPS_PROTOCOL};

    //协议解析
    public static Protocol parseProtocol(String url) {
        if (url.trim().startsWith(FTP_PROTOCOL)) {
            return Protocol.FTP;
        }
        if (url.trim().startsWith(HTTP_PROTOCOL) || url.trim().startsWith(HTTPS_PROTOCOL)) {
            return Protocol.HTTP;
        }
        return null;
    }


    // https://127.0.0.1:9090/abc/123/000070005.ts?ccode=0502&duration=1822 =>abc/123/000070005.ts
    public static String clearPrefixSuffix(String url) {
        String filename = parseFileName(url);
        return parseRelativePath(url) + filename;
    }

    //解析url前缀   http://valipl.net/abc/123/000070005.ts?ccode=0502 =>http://valipl.net/abc/123/
    public static String parsePrefix(String url) {
        if (!url.contains("/")) {
            return url;
        }
        int i = url.lastIndexOf("/") + 1;
        return url.substring(0, i);
    }

    //http://172.1.1.1:9090/zz/yy/xx/index.m3u8 => /zz/yy/xx/
    // /data/a/b/c/index.m3u8  =>/data/a/b/c/
    public static String parseRelativePath(String url) {
        url = rmProtocol(url);
        //带端口的127.0.0.1:9090/abc/123/000070005.ts?ccode=0502
        if (url.contains(":")) {
            int i = url.indexOf("/", url.indexOf(":"));
            return url.substring(i, url.lastIndexOf("/") + 1);
        } else {
            //不带端口的 valipl.net/abc/123/000070005.ts?ccode=0502&
            return url.substring(url.indexOf("/"), url.lastIndexOf("/") + 1);
        }
    }

    // https://127.0.0.1:9090/abc/123/000070005.ts?ccode=0502&duration=1822 =>abc/123/000070005.ts
    public static String parseRelativePathWithFilename(String url) {
        return clearPrefixSuffix(url);
    }


    //解析文件名称 https://valipl.net/000070005.ts?ccode=0502&duration=1822 =>000070005.ts
    public static String parseFileName(String url) {
        //包含?要解析出文件名称
        int i = url.lastIndexOf("/") + 1;
        int j = url.indexOf("?");
        j = j < 0 ? url.length() : j;
      //  log.info("url:{}", url);
        return url.substring(i, j);
    }


    //解析文件后缀
    public static String parseFileSuffix(String url) {
        //包含?要解析出文件名称
        int i = url.lastIndexOf(".") + 1;
        int j = url.indexOf("?");
        j = j < 0 ? url.length() : j;
        return url.substring(i, j);
    }

    private static String rmProtocol(String url) {
        url = url.trim();
        for (String protocol : PROTOCOLS) {
            url = url.replace(protocol.toLowerCase(), "").replace(protocol.toUpperCase(), "");
        }
        return url;
    }

    public static String[] parseFtpUrl(String str) {
        String[] values = null;
        if (str == null) {
            return values;
        }

        String[] strs = str.split("://");
        if (strs.length == 0) {
            return values;
        }
        if (strs.length < 2 || !strs[0].equalsIgnoreCase("ftp")) {
            return values;
        }
        values = new String[7];//增加相对路径和文件名  /a/b/c  和 1.txt
        String address = strs[1].replaceAll("//", "/");
        values[0] = address.substring(0, address.indexOf(':'));
        address = address.substring(address.indexOf(':') + 1, address.length());
        String result = "";
        Pattern p = Pattern.compile("@[0-9.:]{4,25}");
        Matcher m = p.matcher(address);
        if (m.find() && m.toMatchResult().group() != null) {
            result = m.toMatchResult().group().trim();
        } else {
            result = "";
        }
        values[1] = address.split(result)[0];
        values[4] = address.split(result)[1];
        values[4] = values[4].substring(values[4].indexOf('/') + 1);
        values[2] = result.substring(result.indexOf('@') + 1);
        if (values[2].contains(":")) {
            String[] temp = values[2].split(":");
            values[2] = temp[0];
            values[3] = temp[1];
        } else {
            values[3] = "21";
        }

        String temp = values[4]; //  a/b/c/1.txt or 1.txt
        if (StringUtils.hasLength(temp)) {
            int pos1 = temp.lastIndexOf("/");
            if (pos1 > -1) {
                String s1 = "/" + temp.substring(0, pos1);// /a/b/c
                String s2 = temp.substring(pos1 + 1);// 1.txt
                values[5] = s1;
                values[6] = s2;
            } else {
                values[5] = "/";
                values[6] = temp;
            }
        }
        return values;
    }


    public static String clearLine(String line) {
        if (ObjectUtil.isEmpty(line)) return line;
        return line.trim().replace("\r\n", "").replace("\r", "").replace("\n", "");
    }

    public static String clearSlash(String line) {
        if (ObjectUtil.isEmpty(line)) return line;
        return line.trim().replace("///", "/").replace("//", "/");
    }


}
