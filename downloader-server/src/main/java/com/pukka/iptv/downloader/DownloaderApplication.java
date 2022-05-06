package com.pukka.iptv.downloader;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.InetAddress;

/**
 * @author zhengcl
 * @date: 2021/7/23
 */


@SpringBootApplication
//@EnableFeignClients(basePackages = "com.pukka.iptv.common.api.feign")
public class DownloaderApplication implements CommandLineRunner {

    @Value("${server.port}")
    public String port;

    public static void main(String[] args) {
        SpringApplication.run(DownloaderApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Swagger地址：http://" + InetAddress.getLocalHost().getHostAddress() + ":" + port + "/doc.html");
    }
}
