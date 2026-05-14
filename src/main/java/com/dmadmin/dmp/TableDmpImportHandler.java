package com.dmadmin.dmp;

import com.dmadmin.exception.DmAdminException;
import com.dmadmin.exception.ValidationException;

/**
 * 表级 DMP 导入。
 */
public class TableDmpImportHandler extends AbstractDmpImportHandler {

    @Override
    public void execute(DmpImportContext ctx, ImportProgressNotifier notifier) throws DmAdminException {
        if (ctx.getTableName() == null || ctx.getTableName().trim().isEmpty()) {
            throw new ValidationException("表级导入须指定表名（可多表逗号分隔）");
        }
        notifier.notifyProgress(0);
        invoke(ctx, notifier);
    }
}
