package com.pukka.iptv.downloader.nacos;


import com.alibaba.nacos.common.utils.StringUtils;
import com.pukka.iptv.downloader.nacos.model.NacosHost;
import com.pukka.iptv.downloader.nacos.model.NacosNode;
import com.pukka.iptv.downloader.nacos.util.IpUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author WangBO
 * @email wangbo@cjyun.org
 * @date 2021/10/15
 */
@Slf4j
@Component
public class NacosService {

    @Value("${nacos.server-addr}")
    private String NacosServer;
    @Autowired
    private NacosConfig config;

    @Autowired
    @Qualifier("restHttp")
    private RestTemplate restHttp;

    //获取所有 存活实例的id
    public List<String> getAllClearInstanceId() {
        NacosNode serverList = getNacosServerList(config.getServerName(), config.getGroup(), config.getNamespace(), true);
        if (serverList != null) {
            return serverList.getHosts().stream().map(i -> clearInstanceId(i.getInstanceId()))
                    .filter(Objects::nonNull).collect(Collectors.toList());
        }
        return null;
    }

    //获取所有的健康实例
    public List<NacosHost> getAllInstances() {
        NacosNode serverList = getNacosServerList(config.getServerName(), config.getGroup(), config.getNamespace(), true);
        return serverList != null ? serverList.getHosts() : null;
    }

    public String getClearInstanceId() {
        NacosHost host = getCurrentHost();
        return host != null ? clearInstanceId(host.getInstanceId()) : null;
    }

    //获取当前节点的 instanceId
    public NacosHost getCurrentHost() {
        String serverName = config.getServerName();
        String group = config.getGroup();
        String namespace = config.getNamespace();
        Integer port = config.getPort();
        String hostIp = IpUtils.getHostIp();
        return getHost(serverName, group, namespace, hostIp, port.toString());
    }

    /**
     * @param serviceName 服务名
     * @param groupName   分组名
     * @param namespaceId 命名空间ID
     * @param healthyOnly 是否只返回健康实例
     * @return
     */
    public NacosNode getNacosServerList(String serviceName, String groupName, String namespaceId, Boolean healthyOnly) {
        Map<String, String> uriMap = new HashMap<>();
        if (StringUtils.isNotEmpty(serviceName)) {
            uriMap.put("serviceName", serviceName);
        }
        if (StringUtils.isNotEmpty(groupName)) {
            uriMap.put("groupName", groupName);
        }
        if (StringUtils.isNotEmpty(namespaceId)) {
            uriMap.put("namespaceId", namespaceId);
        }
        uriMap.put("healthyOnly", healthyOnly.toString());
        NacosNode nacoslists = null;
        String url = null;
        try {
            url = generateRequestParameters("http", NacosServer + "/nacos/v1/ns/instance/list", uriMap);
            nacoslists = restHttp.getForObject(url, NacosNode.class);
        } catch (RestClientException e) {
            log.error("调用nacos查询服务列表异常,url:{},异常信息：{}", url, e.getMessage());
        }
        return nacoslists;
    }

    /**
     * @param serviceName 服务名
     * @param groupName   分组名
     * @param namespaceId 命名空间ID
     * @param ip          ip地址
     * @param port        端口号
     * @return
     */
    public NacosHost getHost(String serviceName, String groupName, String namespaceId, String ip, String port) {
        Map<String, String> uriMap = new HashMap<>();
        if (StringUtils.isNotEmpty(serviceName)) {
            uriMap.put("serviceName", serviceName);
        }
        if (StringUtils.isNotEmpty(groupName)) {
            uriMap.put("groupName", groupName);
        }
        if (StringUtils.isNotEmpty(namespaceId)) {
            uriMap.put("namespaceId", namespaceId);
        }
        if (StringUtils.isNotEmpty(ip)) {
            uriMap.put("ip", ip);
        }
        if (StringUtils.isNotEmpty(port)) {
            uriMap.put("port", port);
        }
        NacosHost host = new NacosHost();
        String url = null;
        try {
            url = generateRequestParameters("http", NacosServer + "/nacos/v1/ns/instance", uriMap);
            host = restHttp.getForObject(url, NacosHost.class);
        } catch (RestClientException e) {
            log.error("查询nacos服务信息异常 url:{},异常信息：{}", url, e.getMessage());
        }
        return host;
    }

    /**
     * 生成get参数请求url
     * 示例：https://0.0.0.0:80/api?u=u&o=o
     * 示例：https://0.0.0.0:80/api
     *
     * @param protocol 请求协议 示例: http 或者 https
     * @param uri      请求的uri 示例: 0.0.0.0:80
     * @param params   请求参数
     * @return
     */
    public String generateRequestParameters(String protocol, String uri, Map<String, String> params) {
        StringBuilder sb = new StringBuilder(protocol).append("://").append(uri);
        if (params.size() > 0) {
            sb.append("?");
            for (Map.Entry map : params.entrySet()) {
                sb.append(map.getKey())
                        .append("=")
                        .append(map.getValue())
                        .append("&");
            }
            uri = sb.substring(0, sb.length() - 1);
            return uri;
        }
        return sb.toString();
    }

    public static String clearInstanceId(String str) {
        if (str == null) return null;
        return str.replace("#", "_")
                .replace(".", "_")
                .replace("-", "")
                .replace("$", "_")
                .replace("^", "_")
                .replace("*", "_")
                .replace("+", "_")
                .replace("!", "_")
                .replace("~", "_")
                .replace("@", "_");
    }
}
