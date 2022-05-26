package com.lingfeng.biz.server.cache;

import com.lingfeng.biz.downloader.model.DownloadTask;
import com.lingfeng.biz.downloader.model.TaskState;
import com.lingfeng.dutation.store.StoreApi;
import lombok.extern.slf4j.Slf4j;


import java.util.List;

/**
 * @Author: wz
 * @Date: 2022/5/20 13:45
 * @Description:
 */
@Slf4j
public class CacheManage {


    /**
     * @param [storeApi, cacheQueue]
     * @return void
     * @Description 根据cache的实际情况，从持久化api中获取数据，或回写数据
     * @author wz
     * @date 2022/5/26 9:38
     */
    public static void cacheProcess(StoreApi<DownloadTask> storeApi, WaterCacheQueue<DownloadTask> cacheQueue) {
        boolean hungry = cacheQueue.isHungry();
        if (hungry) {
            List<DownloadTask> notJoinCacheList = cacheQueue.addSomeUntilFull(() -> {
                //差值
                int diff = cacheQueue.diff();
                //从存储中获取 待处理 的任务
                //此处需要使用分布式锁,获取N个“待下载”的任务并修改为“下载中”
                return storeApi.queryAndModify(diff, TaskState.WAIT.code(), TaskState.DOING.code());
            });
            //将不能分配的任务还原为“待下载”
            if (notJoinCacheList != null) {
                for (DownloadTask item : notJoinCacheList) {
                    item.setStatus(TaskState.WAIT.code());
                    storeApi.updateById(item);
                }
            }
        }
    }
}
