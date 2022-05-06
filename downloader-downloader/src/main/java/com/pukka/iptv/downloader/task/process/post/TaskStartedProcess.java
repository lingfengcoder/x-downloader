package com.pukka.iptv.downloader.task.process.post;


import com.pukka.iptv.downloader.model.DownloadTask;
import com.pukka.iptv.downloader.model.resp.DownloadNotifyResp;
import org.springframework.stereotype.Component;


/**
 * @Author: wz
 * @Date: 2021/10/30 17:00
 * @Description: 任务开始 通知处理器
 */
@Component
public class TaskStartedProcess extends AbsolutePostProcess {
    @Override
    public DownloadNotifyResp handler(DownloadTask t, boolean doNotify) {
       // FileTask fileTask = t.getFileTask();
       // String notifyUrl = fileTask.getNotifyUrl();
        //生成回调数据
       // DownloadNotifyResp resp = generalNotifyResp(t);
        //开始下载的回调通知
       // super.startedCallback(notifyUrl, resp);
        return null;
    }
}
