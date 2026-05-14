package com.dmadmin.service;

import com.dmadmin.exception.ConnectionFailureException;
import com.dmadmin.model.DbConnectionProfile;
import com.dmadmin.pool.ConnectionPoolManager;
import com.dmadmin.util.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 连接管理服务：注册连接池、测试连接、重试。
 */
public class ConnectionManagementService {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectionManagementService.class);

    private final ConnectionPoolManager poolManager;
    private final AppProperties appProperties;

    /**
     * @param poolManager    连接池单例
     * @param appProperties  应用设定
     */
    public ConnectionManagementService(ConnectionPoolManager poolManager, AppProperties appProperties) {
        this.poolManager = poolManager;
        this.appProperties = appProperties;
    }

    /**
     * 注册连接设定并建立 HikariCP 池。
     *
     * @param profile 连接描述
     * @throws ConnectionFailureException 注册失败
     */
    public void registerConnection(DbConnectionProfile profile) throws ConnectionFailureException {
        poolManager.registerOrUpdate(profile, appProperties);
    }

    /**
     * 执行简单查询测试连接，支援失败重试。
     *
     * @param connectionId 已注册的连接 ID
     * @return 成功则 true
     * @throws ConnectionFailureException 重试后仍失败
     */
    public boolean testConnection(String connectionId) throws ConnectionFailureException {
        int retries = appProperties.getInt("dm.connection.testRetries", 2);
        long delayMs = appProperties.getInt("dm.connection.retryDelayMs", 1000);
        SQLException last = null;
        for (int i = 0; i <= retries; i++) {
            try (Connection c = poolManager.getConnection(connectionId);
                 Statement st = c.createStatement()) {
                st.execute("SELECT 1 FROM DUAL");
                return true;
            } catch (SQLException e) {
                last = e;
                LOG.warn("连接测试失败 (第 {} 次): {}", i + 1, e.getMessage());
                if (i < retries) {
                    sleepQuietly(delayMs);
                }
            }
        }
        throw new ConnectionFailureException("连接测试失败，已重试 " + retries + " 次", last);
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
