package com.lingfeng.biz.downloader.model.media;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

//import javax.validation.constraints.NotBlank;

/**
 * @Author jxm
 * @Date 2021-10-15 16:21
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
public class VideoInfo {
    //@NotBlank(message = "文件名")
    private String fileName;
    //@NotBlank(message = "文件大小")
    private Long fileSize;
    //@NotBlank(message = "视频时长")
    private Long duration;
    //@NotBlank(message = "音频频道类型")
    private Integer audioType;
    //@NotBlank(message = "音频码率")
    private Integer audioBitrate;
    //@NotBlank(message = "视频码率")
    private Integer videoBitrate;
    //@NotBlank(message = "视频编码格式")
    private String videoEncoding;
    //@NotBlank(message = "视频帧率")
    private Float frameRate;
    //@NotBlank(message = "视频分辨率")
    private String resolution;
    //@NotBlank(message = "视频宽")
    private Integer width;
    //@NotBlank(message = "视频高")
    private Integer height;
    //@NotBlank(message = "视频格式")
    private String envelope;
    private String md5;
}
