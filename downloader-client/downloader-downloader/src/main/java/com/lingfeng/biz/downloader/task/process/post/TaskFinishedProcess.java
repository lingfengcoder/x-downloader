package com.lingfeng.biz.downloader.task.process.post;


import com.lingfeng.biz.downloader.model.DTask;
import com.lingfeng.biz.downloader.model.resp.DownloadNotifyResp;
import com.lingfeng.biz.downloader.task.callback.api.FinishedNotify;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


/**
 * @Author: wz
 * @Date: 2021/10/30 17:00
 * @Description: 任务完成 通知处理器
 */
@Slf4j
@Component
public class TaskFinishedProcess extends AbsolutePostProcess {
    //任务完成ack 此处的ack可以有很多种处理
    // 1.只要下载到本地完毕就ack 2.不仅下载完毕，而且后置处理都处理完毕
    @Override
    public DownloadNotifyResp handler(DTask t, boolean doNotify) {
        try {
            FinishedNotify tCallback = t.getCallback();
            if (tCallback != null) {
                tCallback.finish(t);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }
}
