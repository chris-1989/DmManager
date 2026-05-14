package com.dmadmin.dmp;

import com.dmadmin.exception.DmAdminException;

/**
 * 整库 DMP 导入。
 */
public class FullDatabaseDmpImportHandler extends AbstractDmpImportHandler {

    @Override
    public void execute(DmpImportContext ctx, ImportProgressNotifier notifier) throws DmAdminException {
        notifier.notifyProgress(0);
        invoke(ctx, notifier);
    }
}
