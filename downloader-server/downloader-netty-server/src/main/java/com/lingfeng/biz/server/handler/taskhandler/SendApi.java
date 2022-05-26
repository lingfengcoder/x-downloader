package com.lingfeng.biz.server.handler.taskhandler;

import com.lingfeng.biz.downloader.model.DownloadTask;
import com.lingfeng.biz.downloader.model.TaskCmd;
import com.lingfeng.biz.downloader.model.TaskFrame;
import com.lingfeng.biz.server.DownloaderServer;
import com.lingfeng.biz.server.model.NodeClient;
import com.lingfeng.rpc.constant.Cmd;
import com.lingfeng.rpc.data.Frame;
import com.lingfeng.rpc.server.nettyserver.BizNettyServer;
import io.netty.channel.Channel;

/**
 * @Author: wz
 * @Date: 2022/5/20 14:12
 * @Description: 发送消息的API
 */
public class SendApi {

    //发送任务
    public static <M extends DownloadTask> void sendTaskToClient(NodeClient client, M task) {
        Channel channel = client.getChannel();
        BizNettyServer server = DownloaderServer.getInstance();
        TaskFrame<Object> frame = TaskFrame.builder()
                .taskId(task.getId().toString())
                .clientId(client.getClientId())
                .target("listenTask")//rpc目标方法
                .taskCmd(TaskCmd.NEW_TASK)//新任务
                .data(task)
                .build();
        server.writeAndFlush(channel, frame, Cmd.REQUEST);
    }
}
