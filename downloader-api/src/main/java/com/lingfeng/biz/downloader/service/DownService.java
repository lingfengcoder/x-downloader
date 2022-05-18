package com.lingfeng.biz.downloader.service;

import com.lingfeng.biz.downloader.model.FileTask;
import com.lingfeng.biz.downloader.model.resp.R;

import java.io.IOException;

/**
 * @author WangBO
 * @email wangbo@cjyun.org
 * @date 2021/10/14
 */
public interface DownService {
    /**
     * 添加下载任务
     *
     * @param fileTask
     * @return
     * @throws IOException
     */
    R<?> download(FileTask fileTask) throws IOException;

    //文件大小查询
    String fileSizeQuery(String targetUrl, int storeId) throws Exception;

    String fileDelete(String targetUrl, int storeId) throws Exception;

    int getDownloadNodeQueueLen();

    /**
     *
     * @return
     */
    boolean resultCallback();
}
