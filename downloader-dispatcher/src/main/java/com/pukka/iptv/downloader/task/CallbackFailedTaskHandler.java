package com.pukka.iptv.downloader.task;

import com.pukka.iptv.downloader.config.DispatcherConfig;
import com.pukka.iptv.downloader.mq.config.QueueConfig;
import com.pukka.iptv.downloader.mq.model.MsgTask;
import com.pukka.iptv.downloader.mq.model.QueueInfo;
import com.pukka.iptv.downloader.nacos.NacosService;
import com.pukka.iptv.downloader.policy.TrackQueuePolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;


/**
 * @Author: wz
 * @Date: 2021/10/15 11:41
 * @Description: 回调通知失败的处理队列
 */
@Slf4j
//@Component
public class CallbackFailedTaskHandler {

}
