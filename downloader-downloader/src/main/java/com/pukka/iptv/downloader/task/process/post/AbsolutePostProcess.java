package com.pukka.iptv.downloader.task.process.post;

import com.alibaba.fastjson.JSONObject;
import com.pukka.iptv.downloader.config.FtpConfig;
import com.pukka.iptv.downloader.config.NodeConfig;
import com.pukka.iptv.downloader.enums.TaskStatus;
import com.pukka.iptv.downloader.model.*;
import com.pukka.iptv.downloader.model.media.VideoInfo;
import com.pukka.iptv.downloader.model.resp.DownloadNotifyResp;
import com.pukka.iptv.downloader.model.resp.R;
import com.pukka.iptv.downloader.callback.DelayQueueManager;
import com.pukka.iptv.downloader.util.AuthorityUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @Author: wz
 * @Date: 2021/10/30 15:46
 * @Description: 回调后置处理器
 */
@Slf4j
public abstract class AbsolutePostProcess {
    //处理方法
    public abstract DownloadNotifyResp handler(DownloadTask t, boolean doNotify);

    @Autowired
    protected NodeConfig config;
    @Autowired
    protected FtpConfig ftpConfig;
    @Autowired
    private DelayQueueManager delayQueueManager;

    //生成回调数据
    protected DownloadNotifyResp generalNotifyResp(DownloadTask task) {
        FileTask fileTask = task.getFileTask();
        return new DownloadNotifyResp().setFileCode(fileTask.getFileCode())
                .setSourceUrl(fileTask.getSourceUrl())
                .setTargetUrl(fileTask.getTargetUrl())
                .setFinishTime(fileTask.getFinishTime())
                .setSuccess(fileTask.getStatus() == TaskStatus.SUCCESS.getCode());
    }

    //下载任务成功的回调通知
    protected boolean successCallback(String notifyUrl, DownloadNotifyResp param) {
        try {
            R<DownloadNotifyResp> success = R.success(param);
            log.info("【下载成功】回调通知参数:{}", success);
            doNotify(notifyUrl, success);
        } catch (Exception e) {
            log.error("回调异常:{}", e.getMessage());
            return false;
        }
        return true;
    }

    //下载任务失败的回调通知
    protected boolean failedCallback(String notifyUrl, DownloadNotifyResp param, String msg) {
        try {
            R<DownloadNotifyResp> fail = R.fail(msg, param);
            log.info("【下载失败】回调通知参数:{}", fail);
            doNotify(notifyUrl, fail);
        } catch (Exception e) {
            log.error("回调异常:{}", e.getMessage());
            return false;
        }
        return true;
    }

    //任务开始执行的回调
    protected boolean startedCallback(String notifyUrl, DownloadNotifyResp param) {
        try {
            R<DownloadNotifyResp> success = R.success(param);
            log.info("【开始下载】回调通知参数:{}", success);
            doNotify(notifyUrl, success);
        } catch (Exception e) {
            log.error("回调异常:{}", e.getMessage());
            return false;
        }
        return true;
    }

    private void doNotify(String url, R<?> p) {
        delayQueueManager.downCallback(url, JSONObject.toJSONString(p));
    }

    protected static void logDownloadRate(String sourceUrl, String fileLocalPath, Long fileSize, VideoInfo video, long start, long now) {
        if (video != null) {
            computeDownloadRate(sourceUrl, fileLocalPath, video.getFileSize(), start, now);
        } else {
            if (fileSize == null || fileSize == 0) {
                fileSize = getFileSize(fileLocalPath);
            }
            computeDownloadRate(sourceUrl, fileLocalPath, fileSize, start, now);
        }
    }

    protected static void computeDownloadRate(String sourceUrl, String localFile, long size, long start, long now) {
        try {
            BigDecimal time = BigDecimal.valueOf(((float) (now - start) / 1000)).setScale(4, RoundingMode.HALF_UP);
            BigDecimal mb = BigDecimal.valueOf((float) size / 1024 / 1024).setScale(4, RoundingMode.HALF_UP);
            BigDecimal rate = mb.divide(time, 4, RoundingMode.HALF_UP);
            log.info(" sourceUrl:{} localFile:{} 文件大小:{}M,总耗时:{}s,平均下载速率:{}MB/s", sourceUrl, localFile, mb, time, rate);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    protected static long getFileSize(String localFile) {
        try {
            File file = new File(localFile);
            if (file.exists() && file.isFile()) {
                return file.length();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return 0L;
    }

    //将文件和文件所属的文件夹的权限
    protected void setFtpFileAndPathPermission(String fileLocalPath) {
        try {
            FtpAuth ftpAuth = ftpConfig.getFtpAuth();
            //修改文件的权限
            AuthorityUtil.changeFilePermission(fileLocalPath,
                    ftpAuth.getUser(), ftpAuth.getGroup(), AuthorityUtil.PERMISSIONS_755);
            //修改所有父级文件夹的权限
            String parentPath = getParentPath(fileLocalPath);
            while (parentPath != null) {
                AuthorityUtil.changePathPermission(false, parentPath,
                        ftpAuth.getUser(), ftpAuth.getGroup(), AuthorityUtil.PERMISSIONS_755);
                parentPath = getParentPath(parentPath);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    //将文件和文件所属的文件夹的权限
    protected void setM3u8FileAndPathPermission(String fileLocalPath) {
        try {
            log.info("fileLocalPath={}", fileLocalPath);
            FtpAuth ftpAuth = ftpConfig.getFtpAuth();
            String parentPath = getParentPath(fileLocalPath);
            //递归修改指定目录下的所有文件和文件夹的权限修改文件的权限
            AuthorityUtil.changePathAndFilePermission(true, parentPath,
                    ftpAuth.getUser(), ftpAuth.getGroup(), AuthorityUtil.PERMISSIONS_755);
            //修改所有父级文件夹的权限
            while (parentPath != null) {
                AuthorityUtil.changePathPermission(false, parentPath,
                        ftpAuth.getUser(), ftpAuth.getGroup(), AuthorityUtil.PERMISSIONS_755);
                parentPath = getParentPath(parentPath);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    //更新 临时文件 下载完毕标识
    protected void updateDownloading(String fileLocalPath, boolean finish, Downloading backup) {
        Downloading.updateTemp(fileLocalPath, finish, backup);
    }

    protected static String getParentPath(String path) {
        File file = new File(path);
        if (file.exists()) {
            File parent = file.getParentFile();
            if (parent.getAbsolutePath().equalsIgnoreCase(File.separator)) {
                return null;
            }
            if (parent.exists()) {
                return parent.getAbsolutePath();
            } else {
                log.error("父级不存在:{}", path);
            }
        } else {
            log.error("文件不存在: {}", path);
        }
        return null;
    }
}
