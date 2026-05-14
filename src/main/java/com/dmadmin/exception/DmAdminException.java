package com.dmadmin.exception;

/**
 * 达梦管理工具根异常，统一携带可选错误码与原因链。
 */
public class DmAdminException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String errorCode;

    /**
     * 建立一般业务异常。
     *
     * @param message 人类可读说明
     */
    public DmAdminException(String message) {
        super(message);
        this.errorCode = null;
    }

    /**
     * 建立带错误码的业务异常。
     *
     * @param errorCode 内部错误码，便于日志与监控
     * @param message   人类可读说明
     */
    public DmAdminException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 包装底层原因。
     *
     * @param message 说明
     * @param cause   原始异常
     */
    public DmAdminException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }

    /**
     * 包装底层原因并附错误码。
     *
     * @param errorCode 内部错误码
     * @param message   说明
     * @param cause       原始异常
     */
    public DmAdminException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * @return 错误码，可能为 null
     */
    public String getErrorCode() {
        return errorCode;
    }
}
