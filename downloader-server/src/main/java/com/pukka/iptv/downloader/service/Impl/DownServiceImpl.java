package com.pukka.iptv.downloader.service.Impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import com.pukka.iptv.downloader.callback.Resultlistener;
import com.pukka.iptv.downloader.constant.DownloadConstant;
import com.pukka.iptv.downloader.model.Downloading;
import com.pukka.iptv.downloader.model.FileTask;
import com.pukka.iptv.downloader.model.M3u8;
import com.pukka.iptv.downloader.model.resp.DownloadNotifyResp;
import com.pukka.iptv.downloader.model.resp.R;
import com.pukka.iptv.downloader.mq.config.QueueConfig;
import com.pukka.iptv.downloader.mq.model.QueueInfo;
import com.pukka.iptv.downloader.mq.producer.MqSender;
import com.pukka.iptv.downloader.service.DownService;
import com.pukka.iptv.downloader.task.*;
import com.pukka.iptv.downloader.task.process.AsyncDownloadProcess;
import com.pukka.iptv.downloader.task.process.BlockedDownloadProcess;
import com.pukka.iptv.downloader.util.m3u8.M3u8Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author WangBO
 * @email wangbo@cjyun.org
 * @date 2021/10/14
 */
@Component
@Slf4j
public class DownServiceImpl implements DownService {
    @Autowired
    private QueueConfig queueConfig;
    @Autowired
    private AsyncDownloadProcess asyncDownloadProcess;
    @Autowired
    private BlockedDownloadProcess blockedDownloadProcess;
    @Autowired
    private LocalPathTranService localPathTranService;

    @Autowired
    private Resultlistener resultlistener;

    // @PostConstruct
    public void test() {
        //异步下载

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.info("开始压力测试");
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(100);
                    FileTask fileTask = new FileTask()
                            .setFileLocalPath("/data/download/test/").setStoreId(1L)
                            .setAsync(1)
                            .setFileType(1)
                            .setTargetUrl("http://localhost:8080/" + RandomUtil.randomString(6) + "/index.m3u8")
                            .setFileCode(UUID.randomUUID().toString())
                            .setNotifyUrl("http://localhost:7002/api/testCallback");
                    int i = RandomUtil.randomInt(10);
                    if (i % 2 == 0) {
                        fileTask.setSourceUrl("http://1257120875.vod2.myqcloud.com/0ef121cdvodtransgzp1257120875/3055695e5285890780828799271/v.f230.m3u8");
                    } else {
                        fileTask.setSourceUrl("ftp://vstore:iptv!#$vs@172.25.224.110:6069/ts/CNTV/2022/01/04/CCTV-8.ts");
                    }
                    download(fileTask);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                log.info("发送结束");
            }
        }).start();
    }

    @Override
    public R<?> download(FileTask fileTask) {
        //如果是实时下载(阻塞式下载)
        if (fileTask.getAsync() == 2) {
            return syncDownload(fileTask);
        } else {
            //异步下载
            log.info(queueConfig.toString());
            String message = JSON.toJSONString(fileTask);
            QueueInfo queueInfo = new QueueInfo().exchange(queueConfig.getTaskExchange())
                    .routeKey(queueConfig.getTaskRoutingKey());
            log.info(queueInfo.toString());
            MqSender.sendMsg(message, queueInfo, null);
            log.info("下载任务推送成功{}", message);
            return R.success(null);
        }
    }

    // -1:文件不存在
    @Override
    public String fileSizeQuery(String targetUrl, int storeId) throws Exception {
        String localPath = localPathTranService.tranFileLocalPath(targetUrl, storeId);
        //m3u8
        if (ObjectUtil.isNotEmpty(targetUrl) && M3u8.isM3u8Url(targetUrl)) {
            M3u8 m3u8 = M3u8.generalM3u8(targetUrl, localPath, targetUrl);
            m3u8 = M3u8Util.getInstance().getM3u8FinishCount(m3u8);
            if (m3u8 != null) {
                log.info("url:{} total:{} finished:{}", targetUrl, m3u8.getTsTotal(), m3u8.getTsFinishCount());
                return "total:" + m3u8.getTsTotal() + ",finished:" + m3u8.getTsFinishCount();
            }
        }
        //ftp 文件
        else if (targetUrl.toLowerCase().startsWith(DownloadConstant.FTP)) {
            long totalSize = 0;//源文件大小 todo 1.cache起来 2.下载组件生成total文件
            //优先去本地文件获取大小
            if (FileUtil.exist(localPath)) {
                Downloading tmp = Downloading.readTemp(localPath);
                if (tmp != null) {
                    totalSize = tmp.getFileSize();
                }
                File file = new File(localPath);
                return "total:" + totalSize + ",finished:" + FileUtil.size(file);
            }
            //如果本地文件没有，认为下载还没开始或者网络有异常
        }
        //文件不存在
        return "-1";
    }


    //文件删除
    @Override
    public String fileDelete(String url, int storeId) throws Exception {
        boolean result = false;
        String localPath = localPathTranService.tranFileLocalPath(url, storeId);
        //m3u8文件删除
        if (url.toLowerCase().startsWith(DownloadConstant.HTTP)) {
            M3u8 m3u8 = M3u8.generalM3u8(url, localPath, null);
            //m3u8删除
            result = M3u8Util.getInstance().deleteM3u8(m3u8);
        }
        //ftp删除
        else if (url.toLowerCase().startsWith(DownloadConstant.FTP)) {
            //不是目录即可删除
            if (!FileUtil.isDirectory(localPath)) {
                result = FileUtil.del(localPath);
            } else {
                log.error("localPath是目录不能删除{}", localPath);
            }
        }
        return result ? "删除成功" : "删除失败!";
    }

    @Override
    public int getDownloadNodeQueueLen() {
        return asyncDownloadProcess.getAllDownloaderQueueLen();
    }

    @Override
    public boolean resultCallback() {
        return resultlistener.pullData();
    }

    //立即下载文件
    private R<?> syncDownload(FileTask fileTask) {
        DownloadNotifyResp resp = blockedDownloadProcess.download(fileTask);
        if (resp == null) {
            return R.fail("下载失败");
        } else {
            if (resp.isSuccess()) {
                return R.success(resp);
            } else {
                return R.fail(resp.getMsg());
            }
        }
    }


    public static void main(String[] args) {

        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        //ReentrantLock blockLock = new ReentrantLock();
        int cache = 6;
        int x = 10;
        for (int i = 0; i < x; i++) {
            new Thread(() -> {
                ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
                readLock.lock();
                try {
                    Thread thread = Thread.currentThread();
                    log.info("thread:{} 获取到读锁,读到数据{}", thread.getId(), cache);
                    thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                } finally {
                    readLock.unlock();
                }
            }, "thread:" + i).start();
        }

    }


}
