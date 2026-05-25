package com.dmadmin.ui;

import com.dmadmin.model.DbConnectionProfile;

/**
 * 跨分页共用的当前连接信息（注册连接池成功后写入）。
 */
public final class SessionState {

    private volatile String connectionId;
    private volatile DbConnectionProfile profile;

    /**
     * @param connectionId 已注册于 {@link com.dmadmin.pool.ConnectionPoolManager} 的 ID
     */
    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId == null ? null : connectionId.trim();
    }

    /**
     * @return 当前连接 ID，可能为 null 或空字串
     */
    public String getConnectionId() {
        return connectionId;
    }

    /**
     * @return 是否已选定可用连接
     */
    public boolean hasConnection() {
        return connectionId != null && !connectionId.isEmpty();
    }

    /**
     * @param profile 当前连接配置
     */
    public void setConnectionProfile(DbConnectionProfile profile) {
        this.profile = profile;
    }

    /**
     * @return 当前连接配置，可能为 null
     */
    public DbConnectionProfile getConnectionProfile() {
        return profile;
    }
}
