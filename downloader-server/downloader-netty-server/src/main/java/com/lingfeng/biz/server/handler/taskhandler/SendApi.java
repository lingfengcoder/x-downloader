package com.lingfeng.biz.server.handler.taskhandler;

import com.lingfeng.biz.downloader.model.DownloadTask;
import com.lingfeng.biz.downloader.model.TaskCmd;
import com.lingfeng.biz.downloader.model.TaskFrame;
import com.lingfeng.biz.server.DownloaderServer;
import com.lingfeng.biz.server.clientapi.ClientApi;
import com.lingfeng.biz.server.model.NodeClient;
import com.lingfeng.rpc.constant.Cmd;
import com.lingfeng.rpc.invoke.RemoteInvoke;
import com.lingfeng.rpc.server.nettyserver.BizNettyServer;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: wz
 * @Date: 2022/5/20 14:12
 * @Description: 发送消息的API
 */
@Slf4j
public class SendApi {

    //发送任务
    public static <M extends DownloadTask> void sendTaskToClient(NodeClient client, M task) {
        Channel channel = client.getChannel();
        BizNettyServer server = DownloaderServer.getInstance();
        TaskFrame<M> frame = new TaskFrame<>();
        frame.setTaskId(task.getId().toString())
                .setClientId(client.getClientId())
                .setTarget("listenTask")//rpc目标方法
                .setTaskCmd(TaskCmd.NEW_TASK)//新任务
                //.setClazz(DownloadTask.class)
                .setData(task);
        server.writeAndFlush(channel, frame, Cmd.REQUEST);
    }

}
