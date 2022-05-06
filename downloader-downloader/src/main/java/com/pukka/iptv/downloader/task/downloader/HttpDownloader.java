package com.pukka.iptv.downloader.task.downloader;

import com.pukka.iptv.downloader.enums.TaskStatus;
import com.pukka.iptv.downloader.model.*;
import com.pukka.iptv.downloader.task.LocalPathTranService;
import com.pukka.iptv.downloader.task.callback.FtpTaskNotify;
import com.pukka.iptv.downloader.task.callback.api.TaskNotify;
import com.pukka.iptv.downloader.util.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

/**
 * @Author: wz
 * @Date: 2021/10/25 11:19
 * @Description:
 */
@Component
@Slf4j
public class HttpDownloader extends AbstractDownloader {

    @Autowired
    private LocalPathTranService localPathTranService;
    //后置处理器
    @Autowired
    private FtpTaskNotify ftpTaskNotify;

    @Override
    protected TaskNotify getNotify() {
        return ftpTaskNotify;
    }

    /**
     * @Description: 指定任务下载
     * @param: [task]
     * @return: com.pukka.iptv.downloader.model.TaskMsg
     * @author: wz
     * @date: 2021/10/27 20:58
     */
    @Override
    public boolean download(DownloadTask task) {
        matchAndDownload(task);
        return true;
    }

    private DownloadTask matchAndDownload(DownloadTask task) {
        FileTask realTask = task.getFileTask();
        try {
            //默认下载成功
            task.setStatus(DownloadStatus.SUCCESS);
            realTask.setStatus(TaskStatus.SUCCESS.getCode());
            // 下载开始时间
            realTask.setStartTime(new Date());
            // 设置下载中
            realTask.setStatus(TaskStatus.ING.getCode());
            String localPath = localPathTranService.tranFileLocalPath(realTask.getTargetUrl(), realTask.getStoreId());
            //设置文件本地路径
            realTask.setFileLocalPath(localPath);
            download(realTask);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            realTask.setStatus(TaskStatus.FAIL.getCode());
            task.setStatus(DownloadStatus.FAILED);
            task.setMsg(e.getMessage());
        } finally {
            // 下载完成时间
            realTask.setFinishTime(new Date());
            // 计算下载耗;
            realTask.setTimeConsuming(null);
        }
        return task;
    }


    public static void main(String[] args) throws Exception {
        FileTask fileTask = new FileTask()
                .setFileLocalPath("/data/download/test/002656yDIge.jpg").setStoreId(1L)
                .setAsync(2)
                .setFileType(2)
                .setTargetUrl("http://img.netbian.com/file/2022/0329/002656yDIge.jpg")
                .setFileCode(UUID.randomUUID().toString())
                .setNotifyUrl("http://localhost:7002/api/testCallback");
        fileTask.setSourceUrl("http://img.netbian.com/file/2022/0329/002656yDIge.jpg");
        download(fileTask);
    }
    /**
     * @param fileTask 文件任务
     * @Description http文件下载
     * @Author wangbo
     * @Date 2021-10-16 16:43:32
     */
    private static void download(FileTask fileTask) throws Exception {
        String fileCode = fileTask.getFileCode();
        String sourceUrl = fileTask.getSourceUrl();
        Proxy proxy = fileTask.getProxy();
        HttpInfo httpInfo = new HttpInfo().setLocalFilePath(fileTask.getFileLocalPath()).setRemoteUrl(sourceUrl).setFileCode(fileCode);
        if (proxy != null && proxy.isEnable()) {
            //使用 代理下载
            log.info("http使用代理下载");
            HttpUtil.getInstance().downloadProxyHttp(httpInfo, proxy);
        } else {
            HttpUtil.getInstance().downloadHttp(httpInfo);
        }
    }


}
