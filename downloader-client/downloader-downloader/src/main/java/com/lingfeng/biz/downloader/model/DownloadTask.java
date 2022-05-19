package com.lingfeng.biz.downloader.model;

import com.lingfeng.biz.downloader.task.callback.api.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.Objects;

/**
 * @Author: wz
 * @Date: 2021/10/25 10:31
 * @Description:
 */
@Setter
@Getter
@Accessors(chain = true)
public class DownloadTask implements Comparable<DownloadTask> {
    //任务id
    private String taskId;
    //下载节点id(当前下载节点的nacosId)
    private String nodeId;
    //任务详情
    private FileTask fileTask;
    //开始时间
    private Date startTime;
    //结束时间
    private Date endTime;
    //是否是断点下载
    private boolean isBreakPoint;
    // 处理优先级
    private int priority;
    //下载状态
    private DownloadStatus status;
    //下载的一些信息
    private String msg;
    //m3u8文件信息
    private M3u8 m3u8;
    //下载完毕的回调通知
    private FinishedNotify callback;

    @Override
    //设置优先级
    public int compareTo(DownloadTask t) {
        return t.getPriority() - this.getPriority();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DownloadTask task = (DownloadTask) o;
        return Objects.equals(taskId, task.taskId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId);
    }

    // //源文件路径
    //    private String source;
    //    //下载文件路径
    //    private String target;
}
