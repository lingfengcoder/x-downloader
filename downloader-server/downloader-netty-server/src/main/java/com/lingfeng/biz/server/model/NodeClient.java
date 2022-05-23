package com.lingfeng.biz.server.model;

import io.netty.channel.Channel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.lang.ref.WeakReference;

/**
 * @Author: wz
 * @Date: 2022/5/19 16:35
 * @Description: 客户端节点
 */
@Setter
@Getter
@ToString
@Builder
public class NodeClient {

    private WeakReference<Channel> channel;
    //全局唯一的id并且重启后也不能变化 此处和流量路由不同，
    // 因为存在节点与任务的粘连性，没有处理完的任务依然需要之前节点处理
    //所以此处就要求报文中直接携带clientId
    private String clientId;
    //对应channel的id
    private String channelId;

    //是否是激活可用的状态
    private boolean alive;

    //上次修改的时间
    private long modifyTime;

    public Channel getChannel() {
        return this.channel.get();
    }

    public boolean isAlive() {
        Channel channel = getChannel();
        alive = channel != null && channel.isActive();
        return alive;
    }

}
