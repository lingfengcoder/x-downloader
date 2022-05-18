package com.lingfeng.biz.downloader.task.downloader;

import cn.hutool.core.io.FileUtil;
import com.lingfeng.biz.downloader.enums.TaskStatus;
import com.lingfeng.biz.downloader.model.*;
import com.lingfeng.biz.downloader.task.LocalPathTranService;
import com.lingfeng.biz.downloader.task.callback.M3u8TaskNotify;
import com.lingfeng.biz.downloader.task.callback.api.TaskNotify;
import com.lingfeng.biz.downloader.util.m3u8.M3u8Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Date;


/**
 * @Author: wz
 * @Date: 2021/10/25 11:21
 * @Description:
 */
@Slf4j
@Component
public class M3u8Downloader extends AbstractDownloader {
    @Autowired
    private LocalPathTranService localPathTranService;
    @Autowired
    private M3u8TaskNotify notify;

    @Override
    protected TaskNotify getNotify() {
        return notify;
    }

    @Override
    public boolean download(DownloadTask task) {
        downloadM3u8(task);
        return true;
    }

    private boolean downloadM3u8(DownloadTask task) {
        FileTask realTask = task.getFileTask();
        realTask.setStartTime(new Date());
        String targetUrl = realTask.getTargetUrl();
        log.info("m3u8文件开始下载{}", targetUrl);
        M3u8 m3u8 = null;
        try {
            //转换为本地路地址
            Long storeId = realTask.getStoreId();
            String localPath = localPathTranService.tranFileLocalPath(targetUrl, storeId);
            realTask.setFileLocalPath(localPath);
            //初始化m3u8文件
            m3u8 = M3u8.generalM3u8(realTask.getSourceUrl(), localPath, targetUrl);
            Proxy proxy = realTask.getProxy();
            if (proxy != null && proxy.isEnable()) {
                //使用 代理下载
                log.info("m3u8使用代理下载");
                m3u8 = M3u8Util.getInstance().downloadProxyM3u8(m3u8, proxy);
            } else {
                m3u8 = M3u8Util.getInstance().downloadM3u8(m3u8);
            }
            if (m3u8 != null) {
                if (!m3u8.isAllDown()) {
                    task.setStatus(DownloadStatus.FAILED);
                    task.setMsg("文件下载失败(HTTP)-源地址为:" + realTask.getSourceUrl());
                } else {
                    //设置m3u8信息 用于回调通知
                    task.setM3u8(m3u8);
                }
                log.info("生成目标索引文件");
                //m3u8文件下载后 索引文件并不符合targetURL，所以需要将下载后的索引文件数据复制到targetUrl对应的本地文件
                generalLocalIndexFile(m3u8, localPath);
                //重新设置本地文件路径
                m3u8.setLocalFilePath(localPath);
            }
            realTask.setFinishTime(new Date());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            if (m3u8 != null) log.error(m3u8.toString());
            task.setStatus(DownloadStatus.FAILED);
            task.setMsg(e.getMessage());
            realTask.setStatus(TaskStatus.FAIL.getCode());
            return false;
        }
        return true;
    }

    /**
     * @param m3u8, targetUrl, storeId
     * @return void
     * @Description 生成本地索引文件 将下载的m3u8文件 复制到目标索引文件中
     * m3u8文件下载后 索引文件并不符合targetURL，所以需要将下载后的索引文件数据复制到targetUrl对应的本地文件
     * @author wz
     * @date 2021/11/15 10:43
     */
    private void generalLocalIndexFile(M3u8 m3u8, String localPath) {
        try {
            String m3u8Path = m3u8.getLocalFilePath();
            File m3u8File = new File(m3u8Path);
            //如果下载的文件存在
            if (m3u8File.exists()) {
                //下载的索引和本地要生成的索引是同一个文件 直接返回不进行复制
                if (m3u8Path.equalsIgnoreCase(localPath)) {
                    log.info("下载的索引和本地要生成的索引是同一个文件 直接返回不进行复制");
                    return;
                }
                //创建父级目录
                FileUtil.mkParentDirs(localPath);
                File targetFile = new File(localPath);
                boolean newFile = targetFile.createNewFile();
                if (newFile) {
                    FileUtil.copy(m3u8File, targetFile, true);
                    //删除原始m3u8索引文件
                    deleteSourceM3u8Index(m3u8);
                } else {
                    log.error("创建文件失败{}: ", localPath);
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    //删除原始m3u8文件
    private void deleteSourceM3u8Index(M3u8 m3u8) {
        if (!FileUtil.isDirectory(m3u8.getLocalFilePath())) {
            FileUtil.del(m3u8.getLocalFilePath());
        }
    }


}
