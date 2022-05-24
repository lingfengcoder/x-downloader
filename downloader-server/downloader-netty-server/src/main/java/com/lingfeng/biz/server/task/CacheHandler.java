package com.lingfeng.biz.server.task;

import com.lingfeng.biz.downloader.model.DownloadTask;
import com.lingfeng.biz.downloader.model.TaskState;
import com.lingfeng.biz.server.cache.WaterCacheQueue;
import com.lingfeng.dutation.store.StoreApi;
import lombok.extern.slf4j.Slf4j;


import java.util.List;

/**
 * @Author: wz
 * @Date: 2022/5/20 13:45
 * @Description:
 */
@Slf4j
public class CacheHandler {


    public static void cacheHandler(MetaHead head) {
        StoreApi<DownloadTask> storeApi = head.dbStore();
        WaterCacheQueue<DownloadTask> cacheQueue = head.cacheQueue();
        boolean hungry = cacheQueue.isHungry();
        if (hungry) {
            List<DownloadTask> notJoinCacheList = cacheQueue.addSomeUntilFull(() -> {
                //差值
                int diff = cacheQueue.diff();
                return pullDataFromLocalStore(head, diff);
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

    //从 持久化数据中主动拉取数据并添加到缓冲中去
    //todo 分布式锁
    private static List<DownloadTask> pullDataFromLocalStore(MetaHead metaHead, int count) {
        //获取待执行队列信息
        //从存储中获取带处理的任务
        StoreApi<DownloadTask> storeApi = metaHead.dbStore();
        //此处需要使用分布式锁,获取N个“待下载”的任务并修改为“下载中”
        return storeApi.queryAndModify(count, TaskState.WAIT.code(), TaskState.DOING.code());
    }


}
