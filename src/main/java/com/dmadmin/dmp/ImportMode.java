package com.dmadmin.dmp;

/**
 * DMP 导入模式，对应 JNI {@code dll_imp_dm} 第一参数。
 */
public enum ImportMode {

    /** 表级，值为 1 */
    TABLE(1),
    /** 模式级，值为 2 */
    SCHEMA(2),
    /** 整库，值为 3 */
    FULL_DATABASE(3);

    private final int nativeCode;

    ImportMode(int nativeCode) {
        this.nativeCode = nativeCode;
    }

    /**
     * @return 传给原生层的模式整数
     */
    public int getNativeCode() {
        return nativeCode;
    }
}
