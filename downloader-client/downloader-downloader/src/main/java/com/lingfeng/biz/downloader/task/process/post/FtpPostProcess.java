package com.lingfeng.biz.downloader.task.process.post;

import cn.hutool.core.date.SystemClock;
import com.lingfeng.biz.downloader.enums.FileType;
import com.lingfeng.biz.downloader.model.*;
import com.lingfeng.biz.downloader.model.resp.DownloadNotifyResp;
import com.lingfeng.biz.downloader.model.media.PictureInfo;
import com.lingfeng.biz.downloader.model.media.VideoInfo;
import com.lingfeng.biz.downloader.util.FTPUtils;
import com.lingfeng.biz.downloader.util.MediaParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @Author: wz
 * @Date: 2021/10/30 15:43
 * @Description: FTP下载成功处理器
 */
@Component
@Slf4j
public class FtpPostProcess extends AbsolutePostProcess {

    @Override
    //下载成功的回调通知处理器
    public DownloadNotifyResp handler(DTask t, boolean doNotify) {
        if (t == null) return null;
        //下载成功
        log.info("下载后置处理器 开始处理");
        FileTask tmp = t.getFileTask();
        String fileLocalPath = tmp.getFileLocalPath();
        String sourceUrl = tmp.getSourceUrl();
        log.info("对文件和文件夹进行授权");
        setFtpFileAndPathPermission(fileLocalPath);
        log.info("更新临时文件的 下载成功标记");
        updateDownloading(fileLocalPath, true, null);
        log.info("生成回调数据");
        DownloadNotifyResp notifyResp = generalNotifyResp(t);
        FileType trans = FileType.trans(tmp.getFileType());
        long now = SystemClock.now();
        if (trans != null) {
            switch (trans) {
                case VIDEO://视频
                    log.info("对视频进行解析");
                    VideoInfo video = MediaParser.videoAnalysis(tmp.getFileLocalPath());
                    notifyResp.setVideoInfo(video);
                    //打印下载速率
                    logDownloadRate(sourceUrl, fileLocalPath, null, video, t.getStartTime().getTime(), now);
                    break;
                case PICTURE://图片
                    log.info("对图片进行解析");
                    PictureInfo picture = MediaParser.pictureAnalysis(tmp.getFileLocalPath());
                    notifyResp.setPictureInfo(picture);
                    break;
            }
        }
        //如果需要上传到FTP，则进行上传操作
        if (config.getAutoUploadFtp()) {
            log.info("开启了文件上传，上传FTP");
            FTPUtils.ftpUpload(tmp.getTargetUrl(), tmp.getFileLocalPath());
        }
        if (doNotify) {
            log.info("开始下载回调");
            successCallback(tmp.getNotifyUrl(), notifyResp);
        }
        return notifyResp;
    }
}

