package com.dmadmin.dmp;

import com.dmadmin.exception.DmAdminException;
import com.dmadmin.exception.ValidationException;

/**
 * 模式级 DMP 导入。
 */
public class SchemaDmpImportHandler extends AbstractDmpImportHandler {

    @Override
    public void execute(DmpImportContext ctx, ImportProgressNotifier notifier) throws DmAdminException {
        if (ctx.getSchemaName() == null || ctx.getSchemaName().trim().isEmpty()) {
            throw new ValidationException("模式级导入须指定模式名（可多模式逗号分隔）");
        }
        notifier.notifyProgress(0);
        invoke(ctx, notifier);
    }
}
