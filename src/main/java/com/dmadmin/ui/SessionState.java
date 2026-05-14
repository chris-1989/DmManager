package com.dmadmin.ui;

/**
 * 跨分页共用的当前连接 ID（注册连接池成功后写入）。
 */
public final class SessionState {

    private volatile String connectionId;

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
}
