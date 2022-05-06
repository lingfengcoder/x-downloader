package com.pukka.iptv.downloader.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Objects;

/**
 * @Author: wz
 * @Date: 2021/11/8 10:47
 * @Description:
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
public class Proxy {
    private boolean enable;
    private String host;
    private Integer port;
    private String username;
    private String password;
    private String nonHosts;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Proxy proxy = (Proxy) o;
        return Objects.equals(host, proxy.host) &&
                Objects.equals(port, proxy.port) &&
                Objects.equals(username, proxy.username) &&
                Objects.equals(password, proxy.password) &&
                Objects.equals(nonHosts, proxy.nonHosts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port, username, password, nonHosts);
    }
}
