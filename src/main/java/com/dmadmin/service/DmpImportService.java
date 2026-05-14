package com.dmadmin.service;

import com.dmadmin.dmp.DmpImportContext;
import com.dmadmin.dmp.DmpImportHandler;
import com.dmadmin.dmp.DmpImportHandlerFactory;
import com.dmadmin.dmp.DmDimpNativeBridge;
import com.dmadmin.dmp.ImportProgressNotifier;
import com.dmadmin.exception.DmAdminException;
import com.dmadmin.exception.NativeLibraryException;
import com.dmadmin.util.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * DMP 导入服务：加载 JNI、记录选项日志并委派工厂处理器。
 */
public class DmpImportService {

    private static final Logger LOG = LoggerFactory.getLogger(DmpImportService.class);

    private final AppProperties appProperties;
    private final DmpImportHandlerFactory factory = new DmpImportHandlerFactory();

    /**
     * @param appProperties 应用设定（原生库目录、字元集等）
     */
    public DmpImportService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    /**
     * 执行 DMP 导入。
     *
     * @param ctx       导入上下文
     * @param notifier  可选进度通知（可为 null）
     * @throws DmAdminException JNI 或业务失败
     */
    public void importDmp(DmpImportContext ctx, ImportProgressNotifier notifier) throws DmAdminException {
        ImportProgressNotifier n = notifier == null ? new ImportProgressNotifier() : notifier;
//        String libDir = appProperties.getString("dm.dimp.native.library.dir", "").trim();
//        DmDimpNativeBridge.loadLibrary(libDir.isEmpty() ? null : libDir);
        logOptions(ctx, n);
        Charset cs = resolveCharsetFromProperties();
        DmpImportContext effective = new DmpImportContext(
                ctx.getMode(), ctx.getProfile(), ctx.getSchemaName(), ctx.getTableName(),
                ctx.getDmpFilePath(), ctx.getLogFilePath(), ctx.getRemapSchema(),
                ctx.getOptions(), cs,ctx.getDmpToolPath());
        DmpImportHandler handler = factory.create(effective.getMode());
        handler.execute(effective, n);
    }

    private void logOptions(DmpImportContext ctx, ImportProgressNotifier n) {
        String msg = String.format("导入选项: ignoreExisting=%s, data=%s, indexes=%s, constraints=%s, rollback=%s",
                ctx.getOptions().isIgnoreExistingTables(),
                ctx.getOptions().isImportData(),
                ctx.getOptions().isImportIndexes(),
                ctx.getOptions().isImportConstraints(),
                ctx.getOptions().isRollbackOnFailure());
        LOG.info(msg);
        n.notifyLog(msg);
    }

    private Charset resolveCharsetFromProperties() {
        String name = appProperties.getString("dm.dimp.native.charset", "GB18030");
        try {
            return Charset.forName(name.trim());
        } catch (Exception e) {
            LOG.warn("无效字元集 {}, 使用 UTF-8", name);
            return StandardCharsets.UTF_8;
        }
    }
}
