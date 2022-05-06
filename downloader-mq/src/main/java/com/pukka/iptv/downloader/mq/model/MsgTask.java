package com.pukka.iptv.downloader.mq.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Objects;

/**
 * @Author: wz
 * @Date: 2021/10/19 19:51
 * @Description:
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
public class MsgTask {
    private QueueChannel queueChannel;
    private String msg;
    //消息投递的唯一标识
    private long deliveryTag;
    //是否为重试信息
    private boolean isRetryMsg;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MsgTask task = (MsgTask) o;
        return deliveryTag == task.deliveryTag &&
                isRetryMsg == task.isRetryMsg &&
                Objects.equals(queueChannel, task.queueChannel) &&
                Objects.equals(msg, task.msg);
    }

    @Override
    public int hashCode() {
        return Objects.hash(queueChannel, msg, deliveryTag, isRetryMsg);
    }
}
