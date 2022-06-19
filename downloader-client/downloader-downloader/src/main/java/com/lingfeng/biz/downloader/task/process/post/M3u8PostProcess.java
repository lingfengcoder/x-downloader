package com.lingfeng.biz.downloader.task.process.post;


import cn.hutool.core.date.SystemClock;
import com.lingfeng.biz.downloader.enums.TaskStatus;
import com.lingfeng.biz.downloader.model.*;
import com.lingfeng.biz.downloader.model.resp.DownloadNotifyResp;
import com.lingfeng.biz.downloader.model.media.VideoInfo;
import com.lingfeng.biz.downloader.util.MediaParser;
import com.lingfeng.biz.downloader.util.UrlParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @Author: wz
 * @Date: 2021/10/30 15:43
 * @Description: M3U8下载成功处理器
 */
@Slf4j
@Component
public class M3u8PostProcess extends AbsolutePostProcess {

    //m3u8下载回调处理器
    @Override
    public DownloadNotifyResp handler(DTask t, boolean doNotify) {
        if (t == null) return null;
        //下载成功
        FileTask tmp = t.getFileTask();
        tmp.setStatus(TaskStatus.SUCCESS.getCode());
        //生成回调数据
        DownloadNotifyResp notifyResp = generalNotifyResp(t);
        //下载完成的m3u8文件信息
        M3u8 m3u8 = t.getM3u8();
        String localFilePath = m3u8.getLocalFilePath();
        String remoteUrl = m3u8.getRemoteUrl();
        log.info("【M3U8】修改文件权限");
        setM3u8FileAndPathPermission(localFilePath);
        log.info("【M3U8】更新 临时文件 下载完毕标识");
        updateDownloading(localFilePath, true, null);
        log.info("【M3U8】获取ts本地文件路径用于文件解析");
        M3u8.Ts slice = m3u8.getSlice();
        long now = SystemClock.now();
        if (slice != null) {
            log.info("【M3U8】解析其中一个ts文件媒体信息");
            VideoInfo video = MediaParser.videoAnalysis(slice.getLocalPath());
            if (video != null) {
                log.info("【M3U8】重新设置文件名");
                video.setFileName(UrlParser.parseFileName(localFilePath));
                //视频时常和文件大小 采用下载器反馈的数据
                video.setFileSize(m3u8.getFileSize())
                        .setDuration(m3u8.getDuration().longValue());
                notifyResp.setVideoInfo(video);
            }
            //打印下载速率
            logDownloadRate(remoteUrl, null, m3u8.getFileSize(), video, t.getStartTime().getTime(), now);
        }
        if (doNotify) {
            log.info("【M3U8】下载成功回调");
            successCallback(tmp.getNotifyUrl(), notifyResp);
        }
        return notifyResp;
    }
}
