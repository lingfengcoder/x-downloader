package com.pukka.iptv.downloader.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @Author: wz
 * @Date: 2021/10/25 12:02
 * @Description: 下载任务信息类
 */
@Setter
@Getter
@Accessors(chain = true)
public class TaskMsg {
    private DownloadStatus code;
    private String msg;

    public void fail(String msg) {
        this.setCode(DownloadStatus.FAILED).setMsg(msg);
    }

    public static TaskMsg success() {
        return new TaskMsg().setCode(DownloadStatus.SUCCESS);
    }

    public static TaskMsg failed(String msg) {
        return new TaskMsg().setCode(DownloadStatus.FAILED).setMsg(msg);
    }
}
