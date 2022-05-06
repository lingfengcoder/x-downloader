package com.pukka.iptv.downloader.nacos.util;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import cn.hutool.core.date.SystemClock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;

import java.net.*;
import java.util.Enumeration;

/**
 * 获取IP方法
 *
 * @author zhengcl
 */
@Slf4j
public class IpUtils extends ClassicConverter {
    private volatile static String hostAddr;
    private volatile static Long lastUpdateTime;
    private final static String LOCALHOST = "127.0.0.1";

    /**
     * 127.xxx.xxx.xxx 属于"loopback" 地址，即只能你自己的本机可见，就是本机地址，比较常见的有127.0.0.1；
     * 192.168.xxx.xxx 属于private 私有地址(site local address)，属于本地组织内部访问，只能在本地局域网可见。同样10.xxx.xxx.xxx、从172.16.xxx.xxx 到 172.31.xxx.xxx都是私有地址，也是属于组织内部访问；
     * 169.254.xxx.xxx 属于连接本地地址（link local IP），在单独网段可用
     * 从224.xxx.xxx.xxx 到 239.xxx.xxx.xxx 属于组播地址
     * 比较特殊的255.255.255.255 属于广播地址
     * 除此之外的地址就是点对点的可用的公开IPv4地址
     */
    public static String getHostIp() {
        long now = SystemClock.now();
        if (!ObjectUtils.isEmpty(hostAddr) && now - lastUpdateTime < 5000) {
            return hostAddr;
        } else {
            lastUpdateTime = now;
        }
        Enumeration<NetworkInterface> allNetInterfaces;
        //备选地址
        String bakAddr = null;
        try {
            allNetInterfaces = NetworkInterface.getNetworkInterfaces();
            InetAddress inetAddr;
            // 遍历所有的网络接口
            while (allNetInterfaces.hasMoreElements()) {
                NetworkInterface netInterface = allNetInterfaces.nextElement();
                Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
                // 在所有的接口下再遍历IP
                while (addresses.hasMoreElements()) {
                    inetAddr = (InetAddress) addresses.nextElement();
                    if (!inetAddr.isLoopbackAddress()) {// 排除loopback类型地址
                        if (inetAddr.isSiteLocalAddress()) {
                            if (inetAddr instanceof Inet4Address) {
                                // 如果是site-local地址，就是它了
                                bakAddr = inetAddr.getHostAddress();
                                if (!LOCALHOST.equals(bakAddr)) {
                                    hostAddr = bakAddr;
                                    return bakAddr;
                                }
                            }
                        } else if (bakAddr == null) {
                            // site-local类型的地址未被发现，先记录候选地址
                            bakAddr = inetAddr.getHostAddress();
                        }
                    }

                }
            }
        } catch (SocketException e) {
            log.error(e.getMessage(), e);
        }
        //再次尝试获取
        if (ObjectUtils.isEmpty(bakAddr) || LOCALHOST.equals(bakAddr)) {
            try {
                hostAddr = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
        return hostAddr;
    }


    @Override
    public String convert(ILoggingEvent iLoggingEvent) {
        return getHostIp();
    }
}