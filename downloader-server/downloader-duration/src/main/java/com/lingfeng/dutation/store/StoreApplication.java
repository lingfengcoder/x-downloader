package com.lingfeng.dutation.store;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.concurrent.TimeUnit;

/**
 * @author zhengcl
 * @date: 2021/7/23
 */


@SpringBootApplication
@MapperScan("com.lingfeng.dutation.store.mapper")
@EnableTransactionManagement
public class StoreApplication {


    public static void main(String[] args) throws InterruptedException {
        SpringApplication.run(StoreApplication.class, args);

        while (true) {
            TimeUnit.MILLISECONDS.sleep(5000);
        }
    }

}
