package com.lingfeng.biz.downloader.task.process;


import com.lingfeng.biz.downloader.model.DTask;
import com.lingfeng.biz.downloader.model.FileTask;
import com.lingfeng.biz.downloader.model.resp.DownloadNotifyResp;
import com.lingfeng.biz.downloader.task.downloader.AbstractDownloader;
import com.lingfeng.biz.downloader.task.downloader.Downloader;
import com.lingfeng.biz.downloader.task.downloader.FtpDownloader;
import com.lingfeng.biz.downloader.task.downloader.M3u8Downloader;
import com.lingfeng.biz.downloader.task.process.post.FtpPostProcess;
import com.lingfeng.biz.downloader.task.process.post.M3u8PostProcess;
import com.lingfeng.biz.downloader.task.process.post.TaskFailedProcess;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;


/**
 * @Auther: wz
 * @Date: 2021/10/15 11:04
 * @Description: 阻塞下载任务执行处理器
 */
@Slf4j
@Component
@Order(2)
public class BlockedDownloadProcess {

    @Autowired
    private M3u8PostProcess m3u8PostProcess;
    @Autowired
    private FtpPostProcess ftpPostProcess;
    @Autowired
    private TaskFailedProcess taskFailedProcess;

    //同步下载m3u8 不干扰内部下载线程，采用调用者的线程进行下载
    public DownloadNotifyResp download(FileTask fileTask) {
        //创建下载任务
        DTask task = AbstractDownloader.generalTask(fileTask);
        //选择下载器进行下载
        Downloader<DTask> downloader = AbstractDownloader.selectDownloader(task);
        //任务完成ack 此处的ack可以有很多种处理
        // 1.只要下载到本地完毕就ack 2.不仅下载完毕，而且后置处理都处理完毕
        assert downloader != null;
        downloader.preHandler(task);
        try {
            downloader.download(task);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return taskFailedProcess.handler(task, false);
        }
        //后置处理器
        if (M3u8Downloader.class.equals(downloader.getClass())) {
            return m3u8PostProcess.handler(task, false);
        } else if (FtpDownloader.class.equals(downloader.getClass())) {
            return ftpPostProcess.handler(task, false);
        }
        return null;
    }


}
