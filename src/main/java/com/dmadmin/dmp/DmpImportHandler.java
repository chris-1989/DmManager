package com.dmadmin.dmp;

import com.dmadmin.exception.DmAdminException;

/**
 * DMP 导入处理器：依模式组装 JNI 参数并执行。
 */
public interface DmpImportHandler {

    /**
     * 执行导入。
     *
     * @param ctx       导入上下文
     * @param notifier  进度日志通知
     * @throws DmAdminException 导入失败或 JNI 失败
     */
    void execute(DmpImportContext ctx, ImportProgressNotifier notifier) throws DmAdminException;
}
