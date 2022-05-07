package com.lingfeng.register;

import java.util.concurrent.TimeUnit;

/**
 * @Author: wz
 * @Date: 2022/5/7 15:10
 * @Description:
 */
public class ClientMain {

    public static void main(String[] args) {
//        XxlJobExecutor instance = XxlJobExecutor.getInstance();
//        instance.setAdminAddresses("http://127.0.0.1:8080/xxl-job-admin");
//        instance.setAppname("downloader");
//        instance.setPort(9999);
//       instance.mainStart(true);


        XxlJobExecutor instance2 = new XxlJobExecutor();
        instance2.setAdminAddresses("http://127.0.0.1:8080/xxl-job-admin");
        instance2.setAppname("downloader-sender");
        instance2.setPort(8888);

        instance2.mainStart(false);
        while (true) {
            try {
                TimeUnit.HOURS.sleep(1);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
