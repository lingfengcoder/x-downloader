package store;

import com.lingfeng.biz.downloader.model.DownloadTask;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * @Author: wz
 * @Date: 2022/5/18 15:47
 * @Description:
 */
@Component
public class DbStore implements StoreApi<DownloadTask> {
    @Override
    public boolean save(DownloadTask downloadTask) {
        return false;
    }

    @Override
    public List<DownloadTask> query(int limit) {
        return null;
    }

    @Override
    //从数据库中查询limit个 带下载的数据 并修改为下载中
    //note 需要使用分布式锁
    public List<DownloadTask> queryAndModify(int limit, int srcState, int tarState) {
        //模拟从数据库查询
        DownloadTask task = DownloadTask.builder()
                .id(1).url("http://image.gitv.tv//images/0000/00/20210126/cf/cfd3bb76d55b2f68090111ab946edec8.jpg").build();
        return Arrays.asList(task);
    }

    @Override
    public boolean updateById(DownloadTask downloadTask) {
        return false;
    }
}
