package com.dmadmin.exception;

/**
 * 执行 SQL 失败时抛出，通常由 {@link java.sql.SQLException} 转换而来。
 */
public class SqlExecutionException extends DmAdminException {

    private static final long serialVersionUID = 1L;

    /**
     * @param sql     触发失败的 SQL（可截断）
     * @param cause   数据库驱动抛出的异常
     */
    public SqlExecutionException(String sql, Throwable cause) {
        super("SQL", "执行 SQL 失败: " + sql, cause);
    }
}
