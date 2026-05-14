package com.dmadmin.exception;

/**
 * 数据库连接或连接池相关失败时抛出。
 */
public class ConnectionFailureException extends DmAdminException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message 失败原因说明
     */
    public ConnectionFailureException(String message) {
        super("CONN", message);
    }

    /**
     * @param message 失败原因说明
     * @param cause   JDBC 驱动或连接池抛出的异常
     */
    public ConnectionFailureException(String message, Throwable cause) {
        super("CONN", message, cause);
    }
}
