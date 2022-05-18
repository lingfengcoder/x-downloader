package com.lingfeng.biz.downloader.model;


import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * @author wz
 * @date 2022-05-18 16:27:04
 */
@Data
//@TableName("download_task")
@Accessors(chain = true)
public class DownloadTask implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     *
     */
//	@TableId
    private Integer id;
    /**
     * 原始url
     */
    private String url;
    /**
     * 下载次数
     */
    private Integer redoCount;
    /**
     * 下载节点
     */
    private String node;
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
