package com.dmadmin.exception;

/**
 * 输入参数或密码策略等校验不通过时抛出。
 */
public class ValidationException extends DmAdminException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message 校验失败说明
     */
    public ValidationException(String message) {
        super("VALIDATION", message);
    }
}
