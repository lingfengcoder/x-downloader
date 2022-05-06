package com.pukka.iptv.downloader.util;

import com.pukka.iptv.downloader.model.FTPUrlInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @Author: wz
 * @Date: 2022/3/23 16:15
 * @Description:
 */
@Slf4j
public class TestFTP {

    public static void main(String[] args) throws IOException {
        testFtpDataPackage();
    }

    private static void testFtpDataPackage() throws IOException {
        FTPUrlInfo urlInfo = new FTPUrlInfo()
                .setIp("192.168.4.41").setPort(21)
                .setUserName("vstore").setPassword("vspukka");
        FTPClient client = null;
        try {
            client = FTPUtils.ftpLogin(urlInfo);
            //被动模式
            client.enterLocalPassiveMode();
            //主动模式
            //client.enterLocalActiveMode();
            //切换目录
            client.changeWorkingDirectory("/Movie/wztest/demo_mv.mkv");
            client.changeWorkingDirectory("/Movie/wztest");
            //mlist
            FTPFile ftpFile = client.mlistFile("demo_mv.mkv");
            log.info("[mlistFile]->{}", ftpFile);
            //list
            int list = client.list("demo_mv.mkv");
            log.info("[list]->{}", list);
            //listfile
            FTPFile[] listFiles = client.listFiles("demo_mv.mkv");
            log.info("[listFiles]->{}", listFiles);
            //多线程下载大文件测试
            FTPClient finalClient = client;
            new Thread(() -> {
                try {
                    finalClient.setFileType(FTPClient.BINARY_FILE_TYPE);
                    finalClient.setFileTransferMode(FTPClient.STREAM_TRANSFER_MODE);
                    log.info("开始下载文件");
                    finalClient.retrieveFile("demo_mv.mkv", new FileOutputStream(new File("/data/demo.ftp")));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            //穿插测试
//            log.info("开始穿插测试1");
//            listFiles = client.listFiles("demo_mv.mkv");
//            log.info("[listFiles]->{}", listFiles);
//            log.info("开始穿插测试2");
//            listFiles = client.listFiles("demo_mv.mkv");
//            log.info("[listFiles]->{}", listFiles);
//            log.info("开始穿插测试3");
//            listFiles = client.listFiles("demo_mv.mkv");
//            log.info("[listFiles]->{}", listFiles);

            log.info("开始穿插测试cwd");
            client.changeWorkingDirectory("/Movie/wztest");
            client.changeWorkingDirectory("/Movie/wztest");
            client.changeWorkingDirectory("/Movie/wztest");
            client.changeWorkingDirectory("/Movie/wztest");
            TimeUnit.SECONDS.sleep(5);

            log.info("after sleep 开始穿插测试cwd");
            client.changeWorkingDirectory("/Movie/wztest");
            client.changeWorkingDirectory("/Movie/wztest");
            client.changeWorkingDirectory("/");
            client.changeWorkingDirectory("/");
            client.changeWorkingDirectory("/");
            client.changeWorkingDirectory("/");


            log.info("开始穿插测试 listFiles 1");
            listFiles = client.listFiles("demo_mv.mkv");
            log.info("[listFiles]->{}", listFiles);
            log.info("开始穿插测试 listFiles 2");
            listFiles = client.listFiles("demo_mv.mkv");
            log.info("[listFiles]->{}", listFiles);

            listFiles = client.listFiles("demo_mv.mkv");
            log.info("[listFiles2]->{}", listFiles);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            FTPUtils.logout(client);
        }
    }
}
