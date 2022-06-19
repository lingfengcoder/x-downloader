package com.lingfeng.biz.downloader.task.downloader;

import cn.hutool.extra.spring.SpringUtil;

import com.lingfeng.biz.downloader.model.DownloadStatus;
import com.lingfeng.biz.downloader.model.DTask;
import com.lingfeng.biz.downloader.model.FileTask;
import com.lingfeng.biz.downloader.model.M3u8;
import com.lingfeng.biz.downloader.task.callback.api.TaskNotify;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import static com.lingfeng.biz.downloader.constant.DownloadConstant.FTP;
import static com.lingfeng.biz.downloader.constant.DownloadConstant.HTTP;


/**
 * @Author: wz
 * @Date: 2021/12/27 18:40
 * @Description:
 */
@Slf4j
public abstract class AbstractDownloader implements Downloader<DTask> {

    //下载开始通知
    protected abstract TaskNotify getNotify();

    @Override//前置处理器
    public void preHandler(DTask task) {
        try {
            if (task == null) return;
            task.setStartTime(new Date());
            task.setStatus(DownloadStatus.START);
            //Thread.sleep(RandomUtil.randomLong(2000L, 10000L));
            //回调通知
            TaskNotify notify = getNotify();
            //开始下载通知
            if (notify != null) {
                task.setNodeId(task.getFileTask().getTaskServerInstanceId());
                notify.start(task);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override //后置处理器
    public void postHandler(DTask task) {
        try {
            TaskNotify notify = getNotify();
            if (notify != null) {
                try {
                    //任务完成通知  主要用于把消息ack
                    notify.finish(task);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
                if (DownloadStatus.SUCCESS.equals(task.getStatus())) {
                    notify.success(task);
                } else if (DownloadStatus.FAILED.equals(task.getStatus())) {
                    notify.failed(task, task.getMsg());
                }
            }
            task.setEndTime(new Date());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public DTask selectTask(DTask task) {
        return null;
    }

    public static DTask generalTask(FileTask fileTask) {
        //设置任务id
        return new DTask()
                //设置任务id
                .setTaskId(UUID.randomUUID().toString())
                .setFileTask(fileTask)
                .setPriority(fileTask.getPriority());
    }


    //选择下载器
    public static Downloader<DTask> selectDownloader(DTask task) {
        FileTask fileTask = task.getFileTask();
        //如果是m3u8文件 则进行m3u8文件下载
        boolean isM3u8 = M3u8.isM3u8Url(fileTask.getSourceUrl());
        if (isM3u8) {
            return SpringUtil.getBean(M3u8Downloader.class);
        }
        // HTTP下载
        if (fileTask.getSourceUrl().toLowerCase().startsWith(HTTP)) {
            return SpringUtil.getBean(HttpDownloader.class);
        }
        //ftp下载
        else if (fileTask.getSourceUrl().toLowerCase().startsWith(FTP)) {
            return SpringUtil.getBean(FtpDownloader.class);
        }
        return null;
    }
}
