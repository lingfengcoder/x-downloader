package com.pukka.iptv.downloader.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import com.pukka.iptv.downloader.model.Downloading;
import com.pukka.iptv.downloader.model.FTPUrlInfo;
import com.pukka.iptv.downloader.model.Proxy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * @Author jxm
 * @Date 2021-10-13 11:55
 */
@Slf4j
public class FTPUtils {
    // 文件传输类型
    private static final int FILE_TYPE = FTP.BINARY_FILE_TYPE;
    // 缓冲区大小
    private static final int BUFFER_SIZE = 1024;
    // FTP连接超时间
    private static final int CONNECT_TIMEOUT = 1000 * 10;


    //ftp 登录
    public static FTPClient ftpLogin(FTPUrlInfo info) throws IOException {
        return login(new FTPClient(), info.getIp(), info.getPort(), info.getUserName(), info.getPassword());
    }

    //ftp 代理登录
    public static FTPClient ftpProxyLogin(FTPUrlInfo info, Proxy proxy) throws IOException {
        FTPClient ftpClient = new FTPHTTPClient(proxy.getHost(), proxy.getPort(), proxy.getUsername(), proxy.getPassword());
        return login(ftpClient, info.getIp(), info.getPort(), info.getUserName(), info.getPassword());
    }

    public static FTPClient login(String ip, int port, String user, String pwd) throws IOException {
        return login(new FTPClient(), ip, port, user, pwd);
    }

    private static FTPClient login(FTPClient ftpClient, String ip, int port, String user, String pwd) throws IOException {
        try {
            // 被动模式
            ftpClient.enterLocalPassiveMode();
            // 连接超时时间
            ftpClient.setConnectTimeout(CONNECT_TIMEOUT);
            ftpClient.setBufferSize(BUFFER_SIZE);
            // 连接
            ftpClient.connect(ip, port);
            // 登录
            boolean login = ftpClient.login(user, pwd);
            //是否成功登录服务器
            int replyCode = ftpClient.getReplyCode();
            log.info("FTP是否登录成功--------> " + replyCode);
            if (!FTPReply.isPositiveCompletion(replyCode)) {
                //return(reply >= 200 && reply < 300)不是这个范围的就是失败
                log.error("未连接到FTP服务器，用户名或密码错误 " + ip + "_" + port + "_" + user + "_" + pwd);
                //连接失败，更新FTP服务状态
                throw new IOException("FTP服务器登录失败(响应码：" + ftpClient.getReplyCode() + ")");
            }
            if (login) {
                ftpClient.enterLocalPassiveMode();
                // 开启服务器对UTF-8的支持，如果服务器支持就用UTF-8编码，否则就使用本地编码.
                if (FTPReply.isPositiveCompletion(ftpClient.sendCommand("OPTS UTF8", "ON"))) {
                    ftpClient.setControlEncoding(StandardCharsets.UTF_8.name());
                } else {
                    log.error("FTP不支持UTF8编码 {}", ip + "" + pwd);
                    log.info("当前FTPClient编码:{}", ftpClient.getCharset().name());
                }
                log.info("FTP 登录成功！");
            } else {
                // 断开连接
                ftpClient.disconnect();
                log.error("FTP服务器登录失败(响应码：" + ftpClient.getReplyCode() + ") -> 账户或密码错误");
                throw new IOException("FTP服务器登录失败(响应码：" + ftpClient.getReplyCode() + ")");
            }
        } catch (IOException e) {
            log.error("FTP服务器登录失败(响应码：" + ftpClient.getReplyCode() + "){},{}", e.getMessage(), ip + "_" + port + "_" + user + "_" + pwd);
            throw new IOException("FTP服务器登录失败");
        }
        return ftpClient;
    }

    public static void logout(FTPClient client) {
        if (client != null) {
            try {
                client.logout();
                client.disconnect();
                log.info("FTP 退出成功！");
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * @param sourceFtpUrl   地址信息
     * @param targetFilePath 目标文件绝对定位路径
     * @Description 完整下载
     * @Author jxm
     * @Date 2021-10-14 14:40:12
     */
    public static void download(FTPClient ftpClient, String sourceFtpUrl, String targetFilePath) throws IOException {
        download(ftpClient, sourceFtpUrl, targetFilePath, null);
    }

    /**
     * @param :FTP客户端, sourceFtpUrl:源地址, targetFilePath:本地地址,  sourceInfo源文件信息
     * @return void
     * @Description:FTP下载
     * @author wz
     */
    public static void download(FTPClient ftpClient, String sourceFtpUrl, String targetFilePath, SourceInfo notify) throws IOException {
        if (ftpClient == null) return;
        log.info("FTP目标路径：{}", sourceFtpUrl);
        log.info("本地文件路径: {}", targetFilePath);
        //本地文件不存在则创建
        File file = createLocalFileIfNotExist(targetFilePath);
        download(ftpClient, sourceFtpUrl, file, notify);
    }

    public static void download(FTPClient ftpClient, String sourceFtpUrl, File localFile) throws IOException {
        download(ftpClient, sourceFtpUrl, localFile, null);
    }

    /**
     * @param sourceFtpUrl 地址信息
     * @param localFile    目标文件
     * @Description 完整下载
     * @Author jxm
     * @Date 2021-10-14 14:40:12
     */
    public static void download(FTPClient ftpClient, String sourceFtpUrl, File localFile, SourceInfo notify) throws IOException {
        if (ftpClient == null || localFile == null) return;
        String localFilePath = localFile.getAbsoluteFile().getAbsolutePath();
        log.info("FTP目标路径：{}", sourceFtpUrl);
        log.info("本地文件路径: {}", localFilePath);
        Long fileSize;
        //本地文件不存在则创建
        FTPUrlInfo urlInfo = FTPUtils.parseFtpUrlWithCharset(sourceFtpUrl);
        // 目标文件路径
        try (FileOutputStream outputStream = new FileOutputStream(localFile)) {
            FTPFile ftpFile = findFtpFile(ftpClient, urlInfo);
            if (ftpFile != null) {
                fileSize = ftpFile.getSize();
                //file size
                //源文件信息回调
                SourceInfo.call(notify, new Downloading(localFilePath, fileSize));
                // 下载使用二进制
                ftpClient.setFileType(FILE_TYPE);
                // 完整下载
                ftpClient.retrieveFile(urlInfo.getFileName(), outputStream);
            } else {
                throw new IOException("ftp源文件不存在");
            }
        }
    }

    public static void breakpointDownload(FTPClient ftpClient, String sourceFtpUrl, String targetFilePath) throws IOException {
        breakpointDownload(ftpClient, sourceFtpUrl, targetFilePath, null);
    }

    /**
     * @param sourceFtpUrl   地址信息
     * @param targetFilePath 目标文件绝对路径
     * @Description 断点下载
     * @Author jxm
     * @Date 2021-10-14 14:40:12
     */
    public static void breakpointDownload(FTPClient ftpClient, String sourceFtpUrl, String targetFilePath, SourceInfo notify) throws IOException {
        createLocalFileIfNotExist(targetFilePath);
        FTPUrlInfo urlInfo = FTPUtils.parseFtpUrlWithCharset(sourceFtpUrl);
        //找文件
        FTPFile ftpFile = findFtpFile(ftpClient, urlInfo);
        if (ftpFile != null) {
            // 下载使用二进制
            ftpClient.setFileType(FILE_TYPE);
            // 本地文件大小
            Long localFileLength = 0L;
            // 远程文件大小
            long remoteFileLength = 0;
            // 获取本地文件大小
            File file = new File(targetFilePath);
            // 文件大小(byte)
            localFileLength = file.length();
            // 获取远程文件大小
            // 远程文件大小
            remoteFileLength = ftpFile.getSize();
            //源文件信息回调
            SourceInfo.call(notify, new Downloading(targetFilePath, localFileLength));
            // 本地文件大小等于远程
            if (localFileLength >= remoteFileLength) {
                return;
            }
            // 断点下载
            breakpointDownload(ftpClient, localFileLength, urlInfo.getFileName(), targetFilePath);
        } else {
            //log.info("源FTP文件目录不存在：{}", urlInfo.getPath());
            throw new IOException("源FTP文件目录不存在，工作目录切换失败");
        }
    }

    /**
     * @param remoteFilePath 源文件路径
     * @param targetFilePath 目标文件绝对定位路径
     * @Description 断点下载
     * @Author jxm
     * @Date 2021-10-14 14:58:24
     */
    private static void breakpointDownload(FTPClient ftpClient, long offset, String remoteFilePath, String targetFilePath) throws IOException {
        FileOutputStream out = new FileOutputStream(targetFilePath, true);
        InputStream in = ftpClient.retrieveFileStream(remoteFilePath);
        try {
            ftpClient.setRestartOffset(offset);
            byte[] buff = new byte[BUFFER_SIZE];
            int let;
            while ((let = in.read(buff)) != -1) {
                out.write(buff, 0, let);
            }
            // 获取输入输出流是需要确认完成待处理命令
            // 注意retrieveFile、storeFile 不可使用
            ftpClient.completePendingCommand();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } finally {
            IoUtil.closeStream(in, out);
        }
    }


    //删除ftp文件 ftpUrl = ftp://username:pwd@127.0.0.1:8080/abc/123/1.mp4
    public static boolean deleteFile(String ftpUrl) {
        try {
            FTPUrlInfo info = parseFtpUrlWithCharset(ftpUrl);
            FTPClient ftpClient = ftpLogin(info);
            FTPFile ftpFile = findFtpFile(ftpClient, info);
            if (ftpFile != null) {
                if (ftpFile.isFile()) {
                    return ftpClient.deleteFile(info.getFileName());
                } else {
                    log.error("ftpUrl:{} 是目录不能进行删除!", info);
                }
            }
        } catch (Exception e) {
            log.error("delete file exception", e);
        }
        return false;
    }

    //获取文件大小
    public static long getFileSize(String ftpUrl) throws IOException {
        FTPUrlInfo ftpUrlInfo = parseFtpUrlWithCharset(ftpUrl);
        FTPClient ftpClient = Optional.ofNullable(FTPUtils.ftpLogin(ftpUrlInfo)).orElseThrow(() -> new RuntimeException("FTP服务器登录失败"));
        try {
            FTPFile ftpFile = findFtpFile(ftpClient, ftpUrlInfo);
            if (ftpFile != null) {
                return ftpFile.getSize() / 1024;
            } else {
                throw new FileNotFoundException(ftpUrl);
            }
        } catch (IOException e) {
            log.error("工作目录切换失败-路径：{}{}", iso8859ToUtf8(ftpUrlInfo.getPath()), e);
            throw e;
        } finally {
            logout(ftpClient);
        }
    }

    public static boolean ftpUpload(String ftpUrl, String localPath) {
        boolean success = false;
        File file = new File(localPath);
        if (!file.exists()) {
            log.error("本地文件不存在,或路径错误-{}", localPath);
        } else {
            FTPClient ftpClient = null;
            FTPUrlInfo ftpUrlInfo = parseFtpUrlWithCharset(ftpUrl);
            String fileName = ftpUrlInfo.getFileName();
            if (ObjectUtil.isEmpty(ftpUrlInfo.getFileName())) {
                //获取本地文件名称
                String[] split = localPath.split("/");
                fileName = split[split.length - 1];
            }
            try (FileInputStream inputStream = new FileInputStream(file)) {
                ftpClient = Optional.ofNullable(ftpLogin(ftpUrlInfo))
                        .orElseThrow(() -> new IOException("FTP服务器登录失败"));
                // 切换工作目录
                ftpCreateDirectory(ftpClient, ftpUrlInfo.getPath());
                success = ftpClient.storeFile(fileName, inputStream);
            } catch (IOException e) {
                log.error("文件上传失败(FTP)-本地路径:{}{}", localPath, e.getMessage());
            } finally {
                logout(ftpClient);
            }
        }
        return success;
    }


    //找到FTP的文件
    protected static FTPFile findFtpFile(FTPClient ftpClient, FTPUrlInfo info) throws IOException {
        if (ftpClient == null) return null;
        if (ftpClient.changeWorkingDirectory(info.getPath())) {
            //优先通过mlist命令获取文件
            FTPFile file = null;
            try {
                file = ftpClient.mlistFile(info.getFileName());
            } catch (Exception e) {
                log.error("ReplyString= {}", ftpClient.getReplyString());
                log.error(e.getMessage(), e);
            }
            if (file != null) {
                if (file.isFile()) return file;
            } else {
                //如果部分FTP不支持，则使用listfiles命令
                FTPFile[] ftpFiles = ftpClient.listFiles(info.getFileName());
                if (!ObjectUtil.isEmpty(ftpFiles)) {
                    for (FTPFile item : ftpFiles) {
                        if (item.isFile()) {
                            return item;
                        }
                    }
                }
            }
        } else {
            log.error("FTP切换目录失败！{}", info);
        }
        return null;
    }

    //判断文件的目录是否存,不存则创建
    private static void ftpCreateDirectory(FTPClient ftpClient, String path) throws IOException {
        String[] split = path.split("/");
        if (!ObjectUtil.isEmpty(split)) {
            for (String s : split) {
                if (!ObjectUtil.isEmpty(s)) {
                    // 不存在创建
                    if (!ftpClient.changeWorkingDirectory(s)) {
                        ftpClient.makeDirectory(s);
                        ftpClient.changeWorkingDirectory(s);
                    }
                }
            }
        }
    }

    //创建本地文件
    private static File createLocalFileIfNotExist(String path) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            //文件不存在就创建父级目录
            FileUtil.mkParentDirs(file);
            boolean newFile = file.createNewFile();
            if (!newFile) {
                throw new RuntimeException("文件创建失败！" + path);
            }
        }
        return file;
    }

    public static String utf8ToIso8859(String str) {
        return new String(str.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
    }

    public static String iso8859ToUtf8(String str) {
        return new String(str.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
    }

    /**
     * @param url FTP访问地址
     *            示例：
     *            ftp://user:pwd@127.0.0.1:21/
     *            String[0]=user;
     *            String[1]=pwd;
     *            String[2]=127.0.0.1
     *            String[3]=21;
     *            String[4]=a/b/c/1.txt;
     *            String[5]=/a/b/c;
     *            String[6]=1.txt;
     * @Description 地址解析
     * @Author jxm
     * @Date 2021-10-14 14:58:24
     */
    public static FTPUrlInfo parseFTPUrl(String url) {
        try {
            String[] parseResult = UrlParser.parseFtpUrl(url);
            FTPUrlInfo ftpUrlInfo = new FTPUrlInfo();
            String[] fileNameSplit = parseResult[6].split("\\.");
            ftpUrlInfo.setIp(parseResult[2])
                    .setPort(Integer.parseInt(parseResult[3]))
                    .setUserName(parseResult[0])
                    .setPassword(parseResult[1])
                    .setPath(parseResult[5] + "/")
                    .setFileName(parseResult[6])
                    .setFilePath(parseResult[4])
                    .setFormat(fileNameSplit.length > 1 ? fileNameSplit[fileNameSplit.length - 1] : null);
            return ftpUrlInfo;
        } catch (Exception e) {
            log.error("parseFTPUrl url:{}", url);
            throw e;
        }
    }

    //带转码的解析器
    protected static FTPUrlInfo parseFtpUrlWithCharset(String url) {
        FTPUrlInfo ftpUrl = parseFTPUrl(url);
        return ftpUrl.setFileName(utf8ToIso8859(ftpUrl.getFileName()))
                .setFilePath(utf8ToIso8859(ftpUrl.getFilePath()))
                .setFormat(utf8ToIso8859(ftpUrl.getFormat()))
                .setPath(utf8ToIso8859(ftpUrl.getPath()));
    }

}
