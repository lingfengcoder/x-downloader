package com.pukka.iptv.downloader.task.downloader;

import com.pukka.iptv.downloader.enums.TaskStatus;
import com.pukka.iptv.downloader.model.*;
import com.pukka.iptv.downloader.pool.Node;
import com.pukka.iptv.downloader.task.LocalPathTranService;
import com.pukka.iptv.downloader.task.callback.FtpTaskNotify;
import com.pukka.iptv.downloader.task.callback.api.*;
import com.pukka.iptv.downloader.util.FTPUtils;
import com.pukka.iptv.downloader.util.FtpPool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;

/**
 * @Author: wz
 * @Date: 2021/10/25 11:19
 * @Description:
 */
@Component
@Slf4j
public class FtpDownloader extends AbstractDownloader {

    @Autowired
    private LocalPathTranService localPathTranService;
    //后置处理器
    @Autowired
    private FtpTaskNotify ftpTaskNotify;

    @Override
    protected TaskNotify getNotify() {
        return ftpTaskNotify;
    }

    /**
     * @Description: 指定任务下载
     * @param: [task]
     * @return: com.pukka.iptv.downloader.model.TaskMsg
     * @author: wz
     * @date: 2021/10/27 20:58
     */
    @Override
    public boolean download(DownloadTask task) {
        matchAndDownload(task);
        return true;
    }

    private DownloadTask matchAndDownload(DownloadTask task) {
        FileTask realTask = task.getFileTask();
        try {
            //默认下载成功
            task.setStatus(DownloadStatus.SUCCESS);
            // 下载开始时间
            realTask.setStartTime(new Date());
            // 设置下载中
            realTask.setStatus(TaskStatus.ING.getCode());
            String localPath = localPathTranService.tranFileLocalPath(realTask.getTargetUrl(), realTask.getStoreId());
            //设置文件本地路径
            realTask.setFileLocalPath(localPath);
            download(realTask);
            realTask.setStatus(TaskStatus.SUCCESS.getCode());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            realTask.setStatus(TaskStatus.FAIL.getCode());
            task.setStatus(DownloadStatus.FAILED);
            task.setMsg(e.getMessage());
        } finally {
            // 下载完成时间
            realTask.setFinishTime(new Date());
            // 计算下载耗;
            realTask.setTimeConsuming(null);
        }
        return task;
    }


    /**
     * @param fileTask 文件任务
     * @Description FTP文件下载
     * @Author jxm
     * @Date 2021-10-16 16:43:32
     */
    private void download(FileTask fileTask) throws Exception {
        FTPClient ftpClient;
        Node<FtpPool.FKey, FTPClient> node = null;
        String fileCode = fileTask.getFileCode();
        String sourceUrl = fileTask.getSourceUrl();
        boolean breakPoint = fileTask.getRetryCount() > 1;//重试次数大于1次认为要断点续传
        try {
            FTPUrlInfo ftpUrlInfo = FTPUtils.parseFTPUrl(sourceUrl);
            //采用FTP连接池 直接使用线程数个连接 保证每个下载线程都有可用的连接
            FtpPool.FKey key = FtpPool.FKey.general(ftpUrlInfo);
            node = FtpPool.me().pickBlock(key);
            ftpClient = node.getClient();
            log.info("fileCode:{} ftp 开始下载 {}", fileCode, ftpUrlInfo);
            //判断是否需要使用代理下载
            if (breakPoint) {
                log.info("fileCode:{} ftp  断点文件下载 {}", fileCode, ftpUrlInfo);
                FTPUtils.breakpointDownload(ftpClient, sourceUrl, fileTask.getFileLocalPath(), info -> Downloading.generalTemp(info.getFilepath(), info));
            } else {
                // 完整文件下载
                FTPUtils.download(ftpClient, sourceUrl, fileTask.getFileLocalPath(), info -> Downloading.generalTemp(info.getFilepath(), info));
            }
        } catch (IOException e) {
            throw new Exception("fileCode:" + fileCode + " FTP服务异常" + e.getMessage());
        } finally {
            //将连接归还池子
            FtpPool.me().back(node);
        }
    }

  
}
