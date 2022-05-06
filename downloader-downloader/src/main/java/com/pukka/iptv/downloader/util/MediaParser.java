package com.pukka.iptv.downloader.util;

import com.pukka.iptv.downloader.it.sauronsoftware.jave.Encoder;
import com.pukka.iptv.downloader.it.sauronsoftware.jave.MultimediaInfo;
import com.pukka.iptv.downloader.model.TmpMedia;
import com.pukka.iptv.downloader.model.media.PictureInfo;
import com.pukka.iptv.downloader.model.media.VideoInfo;
import lombok.extern.slf4j.Slf4j;

import java.io.*;

/**
 * @Author jxm
 * @Date 2021-10-16 15:56
 * @Description 解析媒体信息工具
 */
@Slf4j
public class MediaParser {

    //视频解析
    public static VideoInfo videoAnalysis(String path) {
        try {
            TmpMedia tmpMedia = analysisMedia(path);
            MultimediaInfo media = tmpMedia.getMedia();
            int width = media.getVideo().getSize().getWidth();
            int height = media.getVideo().getSize().getHeight();
            // 视频
            return new VideoInfo().setFileName(tmpMedia.getFilename())
                    .setFileSize(tmpMedia.getFileSize())// 文件大小
                    .setDuration(media.getDuration())// 视频时长
                    .setAudioType(media.getAudio().getChannels())// 音频类型
                    .setAudioBitrate(media.getAudio().getBitRate())// 音频码率
                    .setVideoBitrate(media.getVideo().getBitRate())// 视频码率
                    .setFrameRate(media.getVideo().getFrameRate())// 视频帧率
                    .setVideoEncoding(media.getVideo().getDecoder())//视频编码格式
                    .setResolution(width + "*" + height)// 分辨率
                    .setWidth(width)// 宽
                    .setHeight(height)// 高
                    .setEnvelope(media.getFormat())// 格式
                    .setMd5(tmpMedia.getMd5());// 文件md5值
        } catch (Exception e) {
            log.error("媒体信息解析失败-目标路径: {} {}", path, e.getMessage());
        }
        return null;
    }

    //图片解析
    public static PictureInfo pictureAnalysis(String path) {
        try {
            TmpMedia tmpMedia = analysisMedia(path);
            MultimediaInfo media = tmpMedia.getMedia();
            // 分辨率
            String resolution = media.getVideo().getSize().getWidth() + "*" + media.getVideo().getSize().getHeight();
            return new PictureInfo().setFileName(tmpMedia.getFilename())
                    .setFileSize(tmpMedia.getFileSize())// 文件大小
                    .setResolution(resolution)// 分辨率
                    .setFormat(media.getFormat())// 格式
                    .setMd5(tmpMedia.getMd5());// 文件md5值
        } catch (Exception e) {
            log.error("媒体信息解析失败-目标路径: {}{}", path, e.getMessage());
        }
        return null;
    }

    //调用工具进行解析
    private static TmpMedia analysisMedia(String path) throws Exception {
        FileInputStream input = null;
        TmpMedia tmpMedia = null;
        File source = null;
        try {
            path = clearPath(path);
            Encoder encoder = new Encoder();
            source = new File(path);
            MultimediaInfo media = encoder.getInfo(source);
            input = new FileInputStream(source);
            //文件大小
            tmpMedia = new TmpMedia()
                    .setMedia(media)
                    .setFilename(source.getName())
                    .setFileSize(input.getChannel().size());
        } finally {
            IoUtil.closeStream(input);
        }
        //计算文件md5
        tmpMedia.setMd5(FileMd5Util.computeMD5(source));
        return tmpMedia;
    }

    private static String clearPath(String path) {
        return path.replace("///", "/").replace("//", "/");
    }


}
