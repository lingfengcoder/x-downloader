package com.lingfeng.biz.downloader;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import java.net.InetAddress;

/**
 * @author zhengcl
 * @date: 2021/7/23
 */


@SpringBootApplication
@ComponentScan(basePackages = "com.lingfeng.**")
@MapperScan("com.lingfeng.dutation.store")
public class DownloaderApplication implements CommandLineRunner {


    public static void main(String[] args) {
        SpringApplication.run(DownloaderApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        //  System.out.println("Swagger地址：http://" + InetAddress.getLocalHost().getHostAddress() + ":" + port + "/doc.html");
    }
}
