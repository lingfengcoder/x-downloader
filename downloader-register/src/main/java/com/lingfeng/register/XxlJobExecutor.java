package com.lingfeng.register;

import com.lingfeng.rpc.EndPointInterface;
import com.lingfeng.rpc.Endpoint;

import com.lingfeng.rpc.EndpointSender;
import com.lingfeng.rpc.model.RegisterDto;
import com.lingfeng.rpc.util.IpUtil;
import com.lingfeng.rpc.util.NetUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;


/**
 * Created by xuxueli on 2016/3/2 21:14.
 */
@Slf4j
public class XxlJobExecutor {

    // ---------------------- param ----------------------
    private String adminAddresses;
    private String accessToken;
    private String appname;
    private String address;
    private String ip;
    private int port;
    private String logPath;
    private int logRetentionDays;

    public void setAdminAddresses(String adminAddresses) {
        this.adminAddresses = adminAddresses;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setAppname(String appname) {
        this.appname = appname;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    public void setLogRetentionDays(int logRetentionDays) {
        this.logRetentionDays = logRetentionDays;
    }

    private static XxlJobExecutor xxlJobExecutor = new XxlJobExecutor();

    public static XxlJobExecutor getInstance() {
        return xxlJobExecutor;
    }

    // ---------------------- start + stop ----------------------
    public void mainStart(boolean isreader) {
        // init executor-server
        //note 初始化 注册中心地址
        initAdminBizList(adminAddresses, accessToken);
        // note rpc服务器
        initEmbedServer(address, ip, port, appname, accessToken, isreader);
    }

    public void destroy() {
        // destory executor-server
        stopEmbedServer();

    }


    // ---------------------- admin-client (rpc invoker) ----------------------
    //note 注册中心集群地址
    private static List<AdminBiz> adminBizList;

    private void initAdminBizList(String adminAddresses, String accessToken) {
        if (adminAddresses != null && adminAddresses.trim().length() > 0) {
            for (String address : adminAddresses.trim().split(",")) {
                if (address != null && address.trim().length() > 0) {

                    AdminBiz adminBiz = new AdminBizClient(address.trim(), accessToken);

                    if (adminBizList == null) {
                        adminBizList = new ArrayList<AdminBiz>();
                    }
                    adminBizList.add(adminBiz);
                }
            }
        }
    }

    public static List<AdminBiz> getAdminBizList() {
        return adminBizList;
    }

    // ---------------------- executor-server (rpc provider) ----------------------
    private EndPointInterface embedServer = null;

    public void initEmbedServer(String address, String ip, int port, String appname, String accessToken, boolean isReader) {

        // fill ip port
        port = port > 0 ? port : NetUtil.findAvailablePort(9999);
        ip = (ip != null && ip.trim().length() > 0) ? ip : IpUtil.getIp();

        // generate address
        if (address == null || address.trim().length() == 0) {
            String ip_port_address = IpUtil.getIpPort(ip, port);   // registry-address：default use address to registry , otherwise use ip:port if address is null
            address = "http://{ip_port}/".replace("{ip_port}", ip_port_address);
        }

        // accessToken
        if (accessToken == null || accessToken.trim().length() == 0) {
            log.warn(">>>>>>>>>>> xxl-job accessToken is empty. To ensure system security, please set the accessToken.");
        }
        if (isReader) {
            // start
            embedServer = new Endpoint();
        } else {
            embedServer = new EndpointSender();
        }
        embedServer.callback((Consumer<RegisterDto>) registerDto ->
                ExecutorRegistryThread.getInstance().start(registerDto.getAppname(), registerDto.getAddress()));
        embedServer.start(address, port, appname, accessToken);

    }

    private void stopEmbedServer() {
        // stop provider factory
        if (embedServer != null) {
            try {
                embedServer.stop();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }


}
