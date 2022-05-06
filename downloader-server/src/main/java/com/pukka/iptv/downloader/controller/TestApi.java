package com.pukka.iptv.downloader.controller;

import cn.hutool.core.io.FileUtil;
import com.pukka.iptv.downloader.config.HttpClient;
import com.pukka.iptv.downloader.model.FTPUrlInfo;
import com.pukka.iptv.downloader.model.M3u8;
import com.pukka.iptv.downloader.model.Proxy;
import com.pukka.iptv.downloader.model.resp.DownloadNotifyResp;
import com.pukka.iptv.downloader.model.resp.R;
import com.pukka.iptv.downloader.util.FTPUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.RestTemplate;

import javax.validation.Valid;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Base64;

/**
 * @Author: wz
 * @Date: 2021/11/24 11:55
 * @Description:
 */
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping(value = "/api/test", produces = MediaType.APPLICATION_JSON_VALUE)
public class TestApi {


    @GetMapping("/testFtpProxy")
    public R<?> testFtpProxy(
            String ftpUrl,
            String proxyHost, Integer proxyPort, String proxyUser, String proxyPwd) throws IOException {
        Proxy proxy = new Proxy().setHost(proxyHost).setPort(proxyPort)
                .setUsername(proxyUser).setPassword(proxyPwd);
        FTPUrlInfo ftpUrlInfo = FTPUtils.parseFTPUrl(ftpUrl);
        FTPClient client = FTPUtils.ftpProxyLogin(ftpUrlInfo, proxy);
        if (client != null) {
            FTPUtils.download(client, ftpUrl, "/data/mmv_proxy.ts");
            return R.success("下载成功");
        } else {
            log.error("client==为空");
        }
        return R.success("下载失败");
    }

    @GetMapping("/testHttpProxy")
    public R<?> testHttpProxy(String url,
                              String proxyHost, Integer proxyPort, String proxyUser, String proxyPwd) throws IOException {
        Proxy proxy = new Proxy().setHost(proxyHost).setPort(proxyPort)
                .setUsername(proxyUser).setPassword(proxyPwd);
        RestTemplate httpClient = HttpClient.getProxyHttp(proxy);

        File file = new File("/data/m3u8/demo.bbq");
        FileUtil.mkParentDirs(file);
        if (!file.exists()) {
            file.createNewFile();
        }
        FileOutputStream out = new FileOutputStream(file);
        RequestCallback callback = request -> request.getHeaders()
                .setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));
        FileOutputStream finalOut = out;
        //获取下载的客户端
        httpClient.execute(url, HttpMethod.GET, callback, resp -> {
            FileCopyUtils.copy(resp.getBody(), finalOut);
            log.info("下载完成:{}", resp.getStatusCode());
            return true;
        });

        try {
            log.info("开始测试m3u8");
            M3u8 m3u8 = null;// M3u8Util.getInstance.(url, "", "/data/m3u8/", proxy);
            if (m3u8 != null) {
                log.info(m3u8.toString());
                return R.success("下载成功");
            } else {
                log.error("client==为空");
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return R.success("下载失败");
    }


    @GetMapping("/testHttpProxy2")
    public R<?> testHttpProxy2(String url,
                               String proxyHost, Integer proxyPort, String proxyUser, String proxyPwd) throws IOException {
        Proxy proxy = new Proxy().setHost(proxyHost).setPort(proxyPort)
                .setUsername(proxyUser).setPassword(proxyPwd);

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(10000);
        factory.setProxy(new java.net.Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress(proxy.getHost(), proxy.getPort())));
        RestTemplate restTemplate = new RestTemplate(factory);

        //认证的账号和密码
        String authentication = proxyUser + ":" + proxyPwd;
        HttpHeaders headers = new HttpHeaders();
        //在请求头信息中携带Basic认证信息(这里才是实际Basic认证传递用户名密码的方式)
        headers.set("Authorization", "Basic " + Base64.getEncoder().encodeToString(authentication.getBytes()));

        //设置编码格式
        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(type);
        //过滤掉账号认证失败的时候抛出的401异常
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                log.error("url:{}", url);
                if (response.getRawStatusCode() != 401) {
                    super.handleError(response);
                }
            }
        });
        //发送请求
        ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET,
                new HttpEntity<byte[]>(headers), String.class);
        log.info("resp body {}", resp.getBody());
        return R.success(resp.getStatusCode());
    }
}
