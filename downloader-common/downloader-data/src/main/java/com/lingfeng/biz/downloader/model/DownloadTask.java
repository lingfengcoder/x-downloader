package com.lingfeng.biz.downloader.model;


import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 任务表
 *
 * @author wz
 * @date 2022-05-19 13:55:19
 */
@Data
@Accessors(chain = true)
@Builder
public class DownloadTask implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     *
     */

    private Integer id;
    /**
     * 原始url
     */
    private String url;
    /**
     * 下载协议 1-http 2-ftp 3-m3u8
     */
    private String type;
    /**
     * 下载次数
     */
    private Integer redoCount;
    /**
     * 下载节点
     */
    private String node;
    /**
     * 下载状态
     */
    private Integer status;
    /**
     * 下载耗时
     */
    private Long costTime;
    /**
     *
     */
    private Date createTime;
    /**
     *
     */
    private Date updateTime;

}
