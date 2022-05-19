package com.lingfeng.biz.downloader.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @Author: wz
 * @Date: 2022/5/19 17:29
 * @Description:
 */
@Setter
@Getter
@ToString
@Builder
public class Route {
    private String clientId;
    private String taskId;
}
