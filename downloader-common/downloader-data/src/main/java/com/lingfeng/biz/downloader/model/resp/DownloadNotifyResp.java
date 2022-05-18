package com.lingfeng.biz.downloader.model.resp;

import com.lingfeng.biz.downloader.model.media.PictureInfo;
import com.lingfeng.biz.downloader.model.media.VideoInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @Author jxm
 * @Date 2021-10-15 16:12
 */
@Getter
@Setter
@Accessors(chain = true)
@ToString
public class DownloadNotifyResp {
    // 文件code
    private String fileCode;
    // 源文件FTP地址
    private String sourceUrl;
    // 下载文件FTP地址
    private String targetUrl;
    // 下载完成时间
    private Date finishTime;
    // 视频信息
    private VideoInfo videoInfo;
    // 图片信息
    private PictureInfo pictureInfo;
    //是否下载成功
    private boolean success;
    private String msg;
    //下载节点id
    private String nodeId;
}
