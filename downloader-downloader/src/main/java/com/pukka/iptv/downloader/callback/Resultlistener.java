package com.pukka.iptv.downloader.callback;


import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.pukka.iptv.downloader.config.ResultConfig;
import com.pukka.iptv.downloader.model.ResultInfo;
import com.pukka.iptv.downloader.mq.consumer.MqPullConsumer;
import com.pukka.iptv.downloader.mq.model.QueueChannel;
import com.pukka.iptv.downloader.mq.model.QueueInfo;
import com.pukka.iptv.downloader.mq.pool.MqUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Resultlistener {
    @Autowired
    private DelayQueueManager delayQueueManager;

    @Autowired
    private ResultConfig resultConfig;


    private QueueInfo getQueueInfo() {
        return new QueueInfo().queue(resultConfig.getResultQueue()).routeKey(resultConfig.getResultRoutekey()).exchange(resultConfig.getResultExchange());
    }

    public boolean pullData() {
        QueueInfo queueInfo = getQueueInfo();
        new MqPullConsumer().name("")
                .mq(queueInfo.fetchCount(50))
                .autoAck(false)//不自动ack
                .autoBackChannel(false)//不自动连接节点归还
                .work(msgTask -> {
                    //添加到缓冲队列中
                    ResultInfo resultInfo = JSONObject.parseObject(msgTask.getMsg(), ResultInfo.class);
                    if (ObjectUtil.isNotEmpty(resultInfo)) {
                        boolean addSuccess = false;
                        try {
                            addSuccess = delayQueueManager.downCallback(resultInfo.getUrl(), resultInfo.getBody());
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                        }
                        if (addSuccess) {
                            //成功就ack掉
                            QueueChannel qc = msgTask.getQueueChannel();
                            long deliveryTag = msgTask.getDeliveryTag();
                            MqUtil.ack(qc.channel(), deliveryTag);
                        } else {
                            //没有添加成功的退回给mq
                            log.info("回调失败，加入延迟队列进行重试");
                        }
                        log.info("下载回调重试成功，信息如下：{}", JSON.toJSON(resultInfo));
                        return true;
                    }
                    return false;
                }).pull();
        return true;
    }
}
