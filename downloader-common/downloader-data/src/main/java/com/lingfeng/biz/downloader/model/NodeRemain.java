package com.lingfeng.biz.downloader.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Objects;

/**
 * @Author: wz
 * @Date: 2021/10/13 18:01
 * @Description:
 */
@Getter
@Setter
@ToString
public class NodeRemain {
    //节点id
    private String clientId;
    //已分配任务个数
    private int remain;
    //节点最多分配的
    private int max;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeRemain that = (NodeRemain) o;
        return Objects.equals(clientId, that.clientId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientId);
    }
}
