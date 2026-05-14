package com.dmadmin.pool;

import com.dmadmin.exception.ConnectionFailureException;
import com.dmadmin.model.DbConnectionProfile;
import com.dmadmin.util.AppProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 连接池单例：依连接设定 ID 快取 {@link HikariDataSource}，支援超时与校验查询。
 */
public final class ConnectionPoolManager {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectionPoolManager.class);
    private static final ConnectionPoolManager INSTANCE = new ConnectionPoolManager();

    private final Map<String, HikariDataSource> pools = new ConcurrentHashMap<>();

    private ConnectionPoolManager() {
    }

    /**
     * @return 全域单例
     */
    public static ConnectionPoolManager getInstance() {
        return INSTANCE;
    }

    /**
     * 注册或更新连接池设定；若已存在同名池则先关闭再重建。
     *
     * @param profile 连接描述
     * @param props   应用程序属性（驱动类名、池参数等）
     * @throws ConnectionFailureException 建立池失败
     */
    public void registerOrUpdate(DbConnectionProfile profile, AppProperties props)
            throws ConnectionFailureException {
        HikariDataSource old = pools.remove(profile.getId());
        if (old != null) {
            old.close();
        }
        try {
            Class.forName(props.getString("dm.jdbc.driverClassName", "dm.jdbc.driver.DmDriver"));
        } catch (ClassNotFoundException e) {
            throw new ConnectionFailureException(
                    "找不到 JDBC 驱动类，请确认 classpath 含 DmJdbcDriver18.jar", e);
        }
        HikariConfig cfg = new HikariConfig();
        cfg.setPoolName("dm-pool-" + profile.getId());
        cfg.setJdbcUrl(profile.buildJdbcUrl());
        cfg.setUsername(profile.getUsername());
        cfg.setPassword(profile.getPassword());
        cfg.setDriverClassName(props.getString("dm.jdbc.driverClassName", "dm.jdbc.driver.DmDriver"));
        cfg.setConnectionTimeout(props.getInt("dm.pool.connectionTimeoutMs", 30000));
        cfg.setValidationTimeout(props.getInt("dm.pool.validationTimeoutMs", 5000));
        cfg.setIdleTimeout(props.getInt("dm.pool.idleTimeoutMs", 600000));
        cfg.setMaxLifetime(props.getInt("dm.pool.maxLifetimeMs", 1800000));
        cfg.setMaximumPoolSize(props.getInt("dm.pool.maximumPoolSize", 10));
        cfg.setMinimumIdle(props.getInt("dm.pool.minimumIdle", 2));
        cfg.setConnectionTestQuery("SELECT 1 FROM DUAL");
        HikariDataSource ds = new HikariDataSource(cfg);
        pools.put(profile.getId(), ds);
        LOG.info("已注册连接池: {}", profile.getId());
    }

    /**
     * @param connectionId 连接设定 ID
     * @return 资料来源
     * @throws ConnectionFailureException 未注册时抛出
     */
    public DataSource getDataSource(String connectionId) throws ConnectionFailureException {
        HikariDataSource ds = pools.get(connectionId);
        if (ds == null) {
            throw new ConnectionFailureException("尚未注册连接池: " + connectionId);
        }
        return ds;
    }

    /**
     * 取得 JDBC 连接（调用方须 try-with-resources 关闭）。
     *
     * @param connectionId 连接设定 ID
     * @return 连接
     * @throws SQLException 取得连接失败
     */
    public Connection getConnection(String connectionId) throws SQLException, ConnectionFailureException {
        return getDataSource(connectionId).getConnection();
    }

    /**
     * 关闭并移除指定连接池。
     *
     * @param connectionId 连接设定 ID
     */
    public void remove(String connectionId) {
        HikariDataSource ds = pools.remove(connectionId);
        if (ds != null) {
            ds.close();
            LOG.info("已关闭连接池: {}", connectionId);
        }
    }

    /**
     * 关闭所有连接池（程序退出时调用）。
     */
    public void shutdownAll() {
        for (Map.Entry<String, HikariDataSource> e : pools.entrySet()) {
            try {
                e.getValue().close();
            } catch (Exception ex) {
                LOG.warn("关闭池失败: {}", e.getKey(), ex);
            }
        }
        pools.clear();
    }
}
