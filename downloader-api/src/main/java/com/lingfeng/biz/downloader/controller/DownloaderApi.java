package com.lingfeng.biz.downloader.controller;


import com.lingfeng.biz.downloader.model.FileTask;
import com.lingfeng.biz.downloader.model.resp.DownloadNotifyResp;
import com.lingfeng.biz.downloader.model.resp.R;
import com.lingfeng.biz.downloader.service.DownService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;

/**
 * @author WangBO
 * @email wangbo@cjyun.org
 * @date 2021/10/14
 */
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class DownloaderApi {
    @Autowired
    private DownService downService;


    //@ApiOperation(value = "添加下载任务")
    @PostMapping("/addTask")
    public R<?> download(@Valid @RequestBody FileTask fileTask) throws IOException {
        return downService.download(fileTask);
    }

    //@ApiOperation(value = "查询文件大小")
    @GetMapping("/file/size/query")
    public R<?> findFileDownloadSchedule(@RequestParam("url") String url,
                                         @RequestParam(required = false) int storeId) throws Exception {
        String result = downService.fileSizeQuery(url, storeId);
        return "-1".equals(result) ? R.fail("文件不存在") : R.success(result);
    }

    //@ApiOperation(value = "删除指定文件")
    @PostMapping("/file/delete")
    public R<String> fileDelete(@RequestParam("url") String url, @RequestParam(required = false) int storeId) throws Exception {
        return R.success(downService.fileDelete(url, storeId));
    }


    @GetMapping("/download/queueLen")
    public R<?> getDownloadNodeQueueLen() {
        int len = downService.getDownloadNodeQueueLen();
        log.info("downloadNodeQueueLen {}", len);
        return R.success(len);
    }


    @PostMapping("/testCallback")
    public R<?> testCallback(@Valid @RequestBody R<DownloadNotifyResp> mediaInfo) {
        log.info("testCallback {}", mediaInfo);
        return R.success(mediaInfo);
    }


    @PostMapping("/resultCallback")
    public R<?> resultCallback(){
        boolean b = downService.resultCallback();
        return b ? R.success(b) : R.fail("重试失败");
    }
}
