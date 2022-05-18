package com.lingfeng.biz.downloader.model;

import lombok.Getter;
import lombok.Setter;

/**
 * @Author: wz
 * @Date: 2021/10/25 18:00
 * @Description: 本地存储配置 下载到挂载目录的配置
 */
@Setter
@Getter
public class LocalStoreInfo {
    private long id;
    private String prefix;
}
