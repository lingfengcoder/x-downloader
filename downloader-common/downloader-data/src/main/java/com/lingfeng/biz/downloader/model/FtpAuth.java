package com.lingfeng.biz.downloader.model;

import lombok.Getter;
import lombok.Setter;

/**
 * @Author: wz
 * @Date: 2021/11/24 14:35
 * @Description:
 */
@Getter
@Setter
public class FtpAuth {
    private String user;
    private String group;
    private String right;
}
