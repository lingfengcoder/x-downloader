package com.lingfeng.biz.downloader.model;

import com.lingfeng.biz.downloader.it.sauronsoftware.jave.MultimediaInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @Author: wz
 * @Date: 2021/10/27 22:30
 * @Description:
 */
@Getter
@Setter
@Accessors(chain = true)
public class TmpMedia {
    private MultimediaInfo media;
    private String filename;
    private Long fileSize;
    private String md5;
}
