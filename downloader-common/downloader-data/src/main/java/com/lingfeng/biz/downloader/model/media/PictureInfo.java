package com.lingfeng.biz.downloader.model.media;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;


/**
 * @Author jxm
 * @Date 2021-10-15 16:21
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
public class PictureInfo {
    //@NotBlank(message = "文件名")
    private String fileName;
    //@NotBlank(message = "文件大小")
    private Long fileSize;
    //@NotBlank(message = "图片分辨率")
    private String resolution;
    //@NotBlank(message = "图片格式")
    private String format;
    private String md5;
}
