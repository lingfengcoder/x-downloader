package com.lingfeng.biz.downloader.task.process.post;

import com.lingfeng.biz.downloader.config.NodeConfig;
import com.lingfeng.biz.downloader.enums.TaskStatus;
import com.lingfeng.biz.downloader.model.DTask;
import com.lingfeng.biz.downloader.model.Downloading;
import com.lingfeng.biz.downloader.model.FileTask;

import com.lingfeng.biz.downloader.model.resp.DownloadNotifyResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @Author: wz
 * @Date: 2021/10/30 15:58
 * @Description: 任务失败处理器
 */
@Slf4j
@Component
public class TaskFailedProcess extends AbsolutePostProcess {
    @Autowired
    private NodeConfig nodeConfig;

    public DownloadNotifyResp handler(DTask t, boolean retry) {
        //下载失败
        FileTask tmp = t.getFileTask();
        tmp.setStatus(TaskStatus.FAIL.getCode());
        //生成回调数据
        DownloadNotifyResp notifyResp = generalNotifyResp(t);
        notifyResp.setMsg(t.getMsg());
        //失败处理
        if (retry) {
            failedTaskHandler(tmp, notifyResp, t.getMsg());
        }
        log.info("下载失败-下载路径为：{}", tmp.getFileLocalPath());
        return notifyResp;
    }

    //失败任务处理
    private void failedTaskHandler(FileTask fileTask, DownloadNotifyResp callbackData, String msg) {
        if (fileTask.getRetryCount() >= nodeConfig.getFailedRetryCount()) {
            // 投入失败队列
            log.info("将任务投入失败队列");
            String fileLocalPath = fileTask.getFileLocalPath();
            log.info("将失败任务的临时下载文件进行索引标记");
            Downloading.updateTemp(fileLocalPath, false, null);
            //note  joinFailedQueue(fileTask);
            failedCallback(fileTask.getNotifyUrl(), callbackData, msg);
        } else {
            // 投入重试队列
            log.info("将任务投入重试队列");
            //note joinRetryQueue(fileTask);
        }
    }


//    //加入失败队列
//    private void joinFailedQueue(FileTask fileTask) {
//        QueueInfo queue = new QueueInfo().exchange(queueConfig.getFailedExchange())
//                .routeKey(queueConfig.getFailedRoutingKey());
//        try {
//            MqSender.sendMsg(JSONObject.toJSONString(fileTask), queue, null);
//        } catch (Exception e) {
//            log.error(e.getMessage(), e);
//        }
//    }
//
//    //加入重试队列
//    private void joinRetryQueue(FileTask fileTask) {
//        QueueInfo queue = new QueueInfo().exchange(queueConfig.getRetryExchange())
//                .routeKey(queueConfig.getRetryRoutingKey());
//        try {
//            MqSender.sendMsg(JSONObject.toJSONString(fileTask), queue, null);
//        } catch (Exception e) {
//            log.error(e.getMessage(), e);
//        } finally {
//            log.info("joinRetryQueue finish");
//        }
//    }

}
