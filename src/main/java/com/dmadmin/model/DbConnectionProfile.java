package com.dmadmin.model;

import java.util.Objects;

/**
 * 单一达梦数据库连接描述，用于建立 JDBC URL 与连接池。
 */
public class DbConnectionProfile {

    private final String id;
    private final String host;
    private final int port;
    private final String username;
    private final String password;

    /**
     * @param id       连接设定唯一识别（用于池快取键）
     * @param host     主机名或 IP
     * @param port     端口号，预设常为 5236
     * @param username 登入使用者
     * @param password 登入密码
     */
    public DbConnectionProfile(String id, String host, int port, String username, String password) {
        this.id = Objects.requireNonNull(id, "id");
        this.host = Objects.requireNonNull(host, "host");
        this.port = port;
        this.username = Objects.requireNonNull(username, "username");
        this.password = password == null ? "" : password;
    }

    public String getId() {
        return id;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    /**
     * @return 达梦 JDBC URL
     */
    public String buildJdbcUrl() {
        return "jdbc:dm://" + host + ":" + port;
    }
}
