package com.lingfeng.biz.downloader.model;

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

    private String msg;
    //消息投递的唯一标识
    private long deliveryTag;
    //是否为重试信息
    private boolean isRetryMsg;
}
