package com.dmadmin.exception;

/**
 * 加载或调用达梦 DMP JNI（DmDexpDimp）失败时抛出。
 */
public class NativeLibraryException extends DmAdminException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message 说明如何配置 DLL 路径等
     */
    public NativeLibraryException(String message) {
        super("NATIVE", message);
    }

    /**
     * @param message 说明
     * @param cause   UnsatisfiedLinkError 等
     */
    public NativeLibraryException(String message, Throwable cause) {
        super("NATIVE", message, cause);
    }
}
