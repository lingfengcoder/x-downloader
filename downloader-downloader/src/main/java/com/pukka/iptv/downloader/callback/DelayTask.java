package com.pukka.iptv.downloader.callback;

import com.pukka.iptv.downloader.model.ResultInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

@Setter
@Getter
@Accessors(chain = true)
public class DelayTask implements Delayed {
    //默认时间单位
    private final static TimeUnit DEFAULT_UNIT = TimeUnit.SECONDS;
    //真实的任务数据
    private ResultInfo data;
    //下次执行时间
    private long expire;
    //执行次数
    private int count;

    public DelayTask(ResultInfo data, long expire) {
        this.data = data;
        this.expire = System.currentTimeMillis() + (expire > 0 ? DEFAULT_UNIT.toMillis(expire) : 0);
    }

    public DelayTask computeTime(long expire) {
        this.expire = System.currentTimeMillis() + (expire > 0 ? DEFAULT_UNIT.toMillis(expire) : 0);
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DelayTask) {
            return this.data.getBody().equals(((DelayTask) obj).getData().getBody());
        }
        return false;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(this.expire - System.currentTimeMillis(), unit);
    }

    @Override
    public int compareTo(Delayed o) {
        long delta = getDelay(TimeUnit.NANOSECONDS) - o.getDelay(TimeUnit.NANOSECONDS);
        return (int) delta;
    }
}
