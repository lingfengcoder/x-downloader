package com.pukka.iptv.downloader.log;


import cn.hutool.extra.spring.SpringUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;


/**
 * @Author: wz
 * @Date: 2021/11/13 17:15
 * @Description: 带控制日志的日志类
 */
@Slf4j
@Component
@Getter
@Setter
@Accessors(chain = true)
public class BizLog {
    @Autowired
    private LogConfig logConfig;

    public static void logs(Consumer<Logger> consumer) {
        BizLog bean = SpringUtil.getBean(BizLog.class);
        bean.log(consumer);
    }

    public void log(Consumer<Logger> consumer) {
        if (logConfig.getLogEnable()) {
            consumer.accept(log);
        }
    }
}
