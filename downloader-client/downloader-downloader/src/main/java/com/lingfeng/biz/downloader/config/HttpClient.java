package com.lingfeng.biz.downloader.config;

import cn.hutool.core.util.ObjectUtil;
import com.lingfeng.biz.downloader.model.Proxy;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Description: 下载器使用的 http客户端
 * @author: wz
 * @date: 2021/11/9 15:33
 */
@Slf4j
public class HttpClient {
    private static final int CONNECTION_REQUEST_TIMEOUT = 15000;
    private static final int CONNECTION_TIMEOUT = 15000;
    private static final int READ_TIMEOUT = 15000;
    private static volatile RestTemplate restTemplate;
    private static volatile ReentrantLock lock = new ReentrantLock();
    //http代理连接池
    private static volatile Map<Proxy, RestTemplate> proxyRestPool = new HashMap<>();


    public static RestTemplate getRestHttp() {
        if (restTemplate == null) {
            lock.lock();
            try {
                if (restTemplate == null) {
                    restTemplate = generalHttpClient();
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            } finally {
                lock.unlock();
            }
        }
        return restTemplate;
    }


    public static RestTemplate getProxyHttp(Proxy proxy) {
        RestTemplate restTemplate = proxyRestPool.get(proxy);
        if (restTemplate == null) {
            lock.lock();
            try {
                restTemplate = proxyRestPool.get(proxy);
                if (restTemplate == null) {
                    restTemplate = generalProxyHttpClient(proxy);
                    proxyRestPool.put(proxy, restTemplate);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            } finally {
                lock.unlock();
            }
        }
        return restTemplate;
    }


    private static RestTemplate generalHttpClient() {
        HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        httpRequestFactory.setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT);
        httpRequestFactory.setConnectTimeout(CONNECTION_TIMEOUT);
        httpRequestFactory.setReadTimeout(READ_TIMEOUT);
        return new RestTemplate(httpRequestFactory);
    }

    private static RestTemplate generalProxyHttpClient(Proxy proxy) {
        ClientHttpRequestFactory factory;
        if (ObjectUtil.isNotEmpty(proxy.getUsername()) && ObjectUtil.isNotEmpty(proxy.getPassword())) {
            factory = generalAuthProxyClient(proxy);
        } else {
            factory = generalNoAuthProxyClient(proxy);
        }
        return new RestTemplate(factory);
    }

    //不带密码认证的代理
    private static ClientHttpRequestFactory generalNoAuthProxyClient(Proxy proxy) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECTION_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        factory.setProxy(new java.net.Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress(proxy.getHost(), proxy.getPort())));
        return factory;
    }

    //带密码认证的代理
    private static ClientHttpRequestFactory generalAuthProxyClient(Proxy proxy) {
        HttpHost myProxy = new HttpHost(proxy.getHost(), proxy.getPort());
        CredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(
                new AuthScope(proxy.getHost(), proxy.getPort()),
                new UsernamePasswordCredentials(proxy.getUsername(), proxy.getPassword())
        );
        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        clientBuilder.setProxy(myProxy).setDefaultCredentialsProvider(provider).disableCookieManagement();
        CloseableHttpClient build = clientBuilder.build();
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT);
        factory.setConnectTimeout(CONNECTION_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        factory.setHttpClient(build);
        return factory;
    }
}
