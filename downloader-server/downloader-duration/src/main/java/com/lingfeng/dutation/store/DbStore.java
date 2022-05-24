package com.lingfeng.dutation.store;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;
import com.lingfeng.biz.downloader.model.DownloadTask;
import com.lingfeng.dutation.store.mapper.DownloadTaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: wz
 * @Date: 2022/5/18 15:47
 * @Description:
 */
@Slf4j
@Component
@MybatisPlusTest
public class DbStore implements StoreApi<DownloadTask> {
    @Autowired
    private DownloadTaskMapper downloadTaskMapper;
    @Resource
    private DbDcsLock dbDcsLock;


    @Override
    public boolean save(DownloadTask downloadTask) {
        return false;
    }

    @Override
    public List<DownloadTask> query(int limit) {
        return null;
    }

    @Override
    //从数据库中查询limit个 待下载的数据 并修改为下载中
    public List<DownloadTask> queryAndModify(int limit, int srcState, int tarState) {
        //模拟从数据库查询
        return dbDcsLock.lock(t -> {
            //查询N个待处理的任务
            LambdaQueryWrapper<DownloadTask> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(DownloadTask::getStatus, srcState).last(" limit   " + limit);
            List<DownloadTask> tmpList = downloadTaskMapper.selectList(queryWrapper);
            if (!tmpList.isEmpty()) {
                //将任务改为处理中
                List<Integer> ids = tmpList.stream().map(DownloadTask::getId).collect(Collectors.toList());
                DownloadTask tmpPo = DownloadTask.builder().status(tarState).build();
                LambdaUpdateWrapper<DownloadTask> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.in(DownloadTask::getId, ids);
                downloadTaskMapper.update(tmpPo, updateWrapper);
            }
            return tmpList;
        });
    }

    @Override
    public boolean updateById(DownloadTask task) {
        DownloadTask build = DownloadTask.builder()
                .id(task.getId())
                .status(task.getStatus())
                .updateTime(new Date())
                .redoCount(task.getRedoCount())
                .build();
        return downloadTaskMapper.updateById(build) > 0;
    }


}
