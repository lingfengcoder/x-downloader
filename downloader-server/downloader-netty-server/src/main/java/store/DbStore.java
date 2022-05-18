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
        return Arrays.asList(new DownloadTask().setId(1)
                .setUrl("http://image.gitv.tv//images/0000/00/20210126/cf/cfd3bb76d55b2f68090111ab946edec8.jpg"));
    }

    @Override
    public boolean updateById(DownloadTask downloadTask) {
        return false;
    }
}
