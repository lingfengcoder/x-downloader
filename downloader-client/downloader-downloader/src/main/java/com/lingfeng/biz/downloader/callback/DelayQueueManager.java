package com.lingfeng.biz.downloader.callback;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lingfeng.biz.downloader.config.ResultConfig;
import com.lingfeng.biz.downloader.model.ResultInfo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@Order(2)
public class DelayQueueManager {
    @Resource(name = "testDelay")
    private ScheduledThreadPoolExecutor testDelaySchedule;
    @Autowired
    // @Qualifier("balanceHttp") //需要指定 用负载的restTemplate
    private RestTemplate balanceHttp;
    @Autowired
    private ResultConfig resultConfig;

    //内置延时队列
    private final DelayQueue<DelayTask> delayQueue = new DelayQueue<>();
    //队列信息
    /**
     * 当延迟队列数大于五十个时，推送到mq中进行容灾保护
     */
    private final static int MAX_DELAY_QUEUE_SIZE = 100;

    //@PostConstruct
    private void init() {

        testDelaySchedule.scheduleAtFixedRate(this::excuteThread, 1, 1000, TimeUnit.MILLISECONDS);

    }

    /**
     * 加入到延时队列中
     *
     * @param task
     */
    private void put(DelayTask task) {
        log.info("加入延时任务：{}", JSON.toJSON(task));
        //超过限制，不在进行重试，直接放到mq里面  可能是回调的目标服务异常时会超出限制
        if (delayQueue.size() > MAX_DELAY_QUEUE_SIZE) {
            //note  MqSender.sendMsg(task.getData(), getQueueInfo());
        } else {
            delayQueue.put(task);
        }
    }

    public boolean downCallback(String url, String param) {
        ResultInfo resultInfo = generaResultTask(url, param);
        boolean result = callback(resultInfo);
        //80%
        if (!result) {
            //20%
            DelayTask task = generalTask(url, param, resultConfig.getFirstCallbackTime());
            put(task);
            return false;
        }
        return true;
    }

    private DelayTask generalTask(String url, String param, Long time) {
        return new DelayTask(new ResultInfo().setUrl(url).setBody(param), time);
    }

    private ResultInfo generaResultTask(String url, String param) {
        return new ResultInfo().setUrl(url).setBody(param);
    }

    private boolean work(DelayTask task) {
        ResultInfo data = task.getData();
        task.setCount(task.getCount() + 1);
        return callback(data);
    }

    private boolean callback(ResultInfo resultInfo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> result = balanceHttp.postForEntity(resultInfo.getUrl(), new HttpEntity<>(resultInfo.getBody(), headers), String.class);
        return HttpStatus.OK.equals(result.getStatusCode());
    }

    /**
     * 取消延时任务
     *
     * @param task
     * @return
     */
    public boolean remove(DelayTask task) {
        log.info("取消延时任务：{}", task);
        return delayQueue.remove(task);
    }

    //@PostConstruct
    public void test() {
        String body = "这是一条消息";
        String url = "www.baidu.com";
        ResultInfo resultInfo = new ResultInfo().setBody(body).setUrl(url);
        this.put(new DelayTask(resultInfo, 30));
    }

    /**
     * 延时任务执行线程
     */
    private void excuteThread() {
        DelayTask task = null;
        try {
            task = delayQueue.take();
            boolean result = this.work(task);
            if (result) {
                log.info("回调成功！{}", JSONObject.toJSONString(task));
            } else {
                switch (task.getCount()) {
                    //当执行一次之后 还是失败
                    //after once
                    case 2:
                        //重新计算下次执行时间
                        task.computeTime(resultConfig.getSecondCallbackTime());
                        this.put(task);
                        break;
                    case 3:
                        task.computeTime(resultConfig.getThirdCallbackTime());
                        this.put(task);
                        break;
                    default:
                        //note MqSender.sendMsg(task.getData(), getQueueInfo());
                        break;
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 内部执行延时任务
     *
     * @param task
     */
    private void processTask(DelayTask task) {
        log.info("执行延时任务：{}", task);
        //根据task中的data自定义数据来处理相关逻辑，例 if (task.getData() instanceof XXX) {}
    }
}
