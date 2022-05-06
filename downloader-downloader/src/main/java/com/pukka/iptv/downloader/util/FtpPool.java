package com.pukka.iptv.downloader.util;


import cn.hutool.extra.spring.SpringUtil;
import com.pukka.iptv.downloader.config.NodeConfig;
import com.pukka.iptv.downloader.model.FTPUrlInfo;
import com.pukka.iptv.downloader.model.Proxy;

import com.pukka.iptv.downloader.nacos.listener.NacosListener;
import com.pukka.iptv.downloader.nacos.listener.NacosNotify;
import com.pukka.iptv.downloader.pool.AbstractPool;
import com.pukka.iptv.downloader.pool.Key;
import com.pukka.iptv.downloader.pool.Node;
import com.pukka.iptv.downloader.pool.PoolConfig;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author: wz
 * @Date: 2021/11/8 22:08
 * @Description: FTP连接池
 */
@Slf4j
public class FtpPool extends AbstractPool<FtpPool.FKey, FTPClient, Node<FtpPool.FKey, FTPClient>> implements NacosNotify {
    private final static ReentrantLock lock = new ReentrantLock(true);
    //连接池
    private final static Map<FKey, Collection<Node<FtpPool.FKey, FTPClient>>> pool = new HashMap<>();
    //每个KEY-FTP限制数
    private final static int DEFAULT_KEY_POOL_SIZE = 5;
    //每个FTP最大限制数
    private final static int MAX_LIMIT = NO_LIMIT;
    //最大存活时间　5s
    private final static long MAX_LIVE_TIME = 5000;
    //最大等待队列长度
    private final static int MAX_AWAIT_QUEUE_LENGTH = 1024;

    private static final FtpPool me = new FtpPool();

    static {
        //注册到通知
        NacosListener.register(me);
    }

    private FtpPool() {
    }

    public static FtpPool me() {
        return me;
    }

    @Override
    protected PoolConfig<FKey, FTPClient, Node<FKey, FTPClient>> getPoolConfig() {
        return new PoolConfig<FKey, FTPClient, Node<FKey, FTPClient>>()
                .setName("biz-ftp-pool")
                .setPool(pool).setLock(lock)
                .setEnableSchedule(true)//自动关闭过期连接
                .setAwaitQueueLength(MAX_AWAIT_QUEUE_LENGTH)
                .setMaxLiveNodeLimit(MAX_LIMIT)
                .setMaxFreeNodeLiveTime(MAX_LIVE_TIME);
    }

    @Override
    //建立一个连接
    protected Node<FKey, FTPClient> generalConnect(FKey key) {
        try {
            FTPClient client = null;
            FTPUrlInfo ftpUrl = key.getFtpUrl();
            Proxy proxy = key.getProxy();
            if (proxy == null) {
                client = FTPUtils.ftpLogin(ftpUrl);
            } else if (proxy.isEnable()) {
                client = FTPUtils.ftpProxyLogin(ftpUrl, proxy);
            }
            return Node.node(key, client);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    //关闭连接
    protected boolean closeConnect(Node<FKey, FTPClient> node) {
        FTPClient client = node.getClient();
        FTPUtils.logout(client);
        return true;
    }

    @Override
    //测试连接是否可用
    public boolean nodeIsOpen(Node<FKey, FTPClient> node) {
        try {
            node.getClient().getStatus();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return false;
        }
        return true;
    }

    //刷新最大连接数
    public void refreshMaxLimit(int limit) {
        PoolConfig<FKey, FTPClient, Node<FKey, FTPClient>> config =
                new PoolConfig<FKey, FTPClient, Node<FKey, FTPClient>>()
                        .setMaxLiveNodeLimit(limit);
        refreshPoolConfig(config);
    }

    //动态调整每个key的limit
    public void refreshKeyLimit(int limit) {
        Set<FKey> keys = getAllKeys();
        forceRefreshKeyPoolLimit(new ArrayList<>(keys), limit);
    }

    @Override
    //监听nacos配置调整
    public void configRefreshEvent() {
        refreshKeyLimit(limit());
    }

    private static int limit() {
        try {
            NodeConfig bean = SpringUtil.getBean(NodeConfig.class);
            //由于除了内置的通过nacos配置的连接(concurrentLimit)需要使用到FTP连接池，
            //在downloader-server中提供了阻塞式的下载模式(不通过dispatcher，直接用tocmat线程进行阻塞下载)，依然需要使用FTP连接池，
            // 所以这里给出了两倍的限制数
            int limit = bean.getConcurrentLimit() * 2;
            return limit <= 0 ? DEFAULT_KEY_POOL_SIZE : limit;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        //默认
        return DEFAULT_KEY_POOL_SIZE;
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    //连接节点的唯一标识 注意一定要重写equals和hashcode方法
    public static class FKey implements Key<FKey> {
        private FTPUrlInfo ftpUrl;
        //代理信息
        private Proxy proxy;
        //每个key同时最多有limit个连接
        private int limit;

        @Override
        public int getLimit() {
            //通过配置获取
            this.limit = limit();
            return this.limit;
        }

        @Override
        public FKey setLimit(int limit) {
            this.limit = limit;
            return this;
        }

        @Override
        public String getName() {
            return ftpUrl.toString();
        }

        @Override
        public FKey cloneMe() {
            FKey fKey = new FKey();
            fKey.setFtpUrl(ftpUrl);
            fKey.setProxy(proxy);
            fKey.setLimit(limit);
            return fKey;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FKey key = (FKey) o;
            FTPUrlInfo other = key.getFtpUrl();
            Proxy otherProxy = key.getProxy();
            //ftp  eq
            boolean ftpFlag = false;
            if (this.ftpUrl != null && other != null) {
                ftpFlag = Objects.equals(this.ftpUrl.getIp(), other.getIp()) &&
                        Objects.equals(this.ftpUrl.getPort(), other.getPort()) &&
                        Objects.equals(this.ftpUrl.getUserName(), other.getUserName()) &&
                        Objects.equals(this.ftpUrl.getPassword(), other.getPassword());
            } else if (this.ftpUrl == null && other == null) {
                ftpFlag = true;
            }

            //proxy eq
            boolean proxyFlag = false;
            if (this.proxy != null && otherProxy != null) {
                proxyFlag = Objects.equals(this.proxy.isEnable(), otherProxy.isEnable()) &&
                        Objects.equals(this.proxy.getHost(), otherProxy.getHost()) &&
                        Objects.equals(this.proxy.getPort(), otherProxy.getPort()) &&
                        Objects.equals(this.proxy.getUsername(), otherProxy.getUsername()) &&
                        Objects.equals(this.proxy.getPassword(), otherProxy.getPassword());
            } else if (this.proxy == null && otherProxy == null) {
                proxyFlag = true;
            }
            return ftpFlag && proxyFlag;
        }

        @Override
        public int hashCode() {
            String hash = ftpUrl.getIp() + ftpUrl.getPort() + ftpUrl.getUserName() + ftpUrl.getPassword();
            if (proxy != null) {
                hash += proxy.isEnable() + proxy.getHost() + proxy.getPort() + proxy.getUsername() + proxy.getPassword();
            }
            return Objects.hash(hash);
        }

        public static FKey general(FTPUrlInfo ftpUrl) {
            return new FKey().setFtpUrl(ftpUrl);
        }

        public static FKey general(FTPUrlInfo ftpUrl, Proxy proxy) {
            return new FKey().setFtpUrl(ftpUrl).setProxy(proxy);
        }
    }


    //test
    public static void main(String[] args) {
        int count = 100;
        FTPUrlInfo ftpUrlInfo = new FTPUrlInfo()
                .setIp("172.25.224.110").setPort(6069)
                .setUserName("vstore").setPassword("iptv!#$vs");
        FKey key = FKey.general(ftpUrlInfo).setLimit(5);
        for (int i = 0; i < count; i++) {
            if (i > 9) {
                log.info("调整连接限制为10 ");
                FtpPool.me().refreshKeyLimit(10);
            }
            new Thread(() -> {
                FtpPool ftpPool = FtpPool.me();
                Thread thread = Thread.currentThread();
                // Node<Key, FTPClient> node = ftpPool.pick(key, 6000);
                Node<FKey, FTPClient> node = ftpPool.pickBlock(key);
                log.info("thread {} 号　获取了连接{}", thread.getId(), node.hashCode());
                FTPClient client = node.getClient();
                try {
                    FTPFile[] ftpFiles = client.listFiles();
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                log.info("thread {} 号　释放了连接 {}", thread.getId(), node.hashCode());
                ftpPool.back(node);
            }).start();
        }
    }

}
