package com.dmadmin.dmp;

import com.dmadmin.model.DbConnectionProfile;
import com.dmadmin.model.DmpImportOptions;

import java.nio.charset.Charset;
import java.util.Objects;

/**
 * 单次 DMP 导入上下文：连接、档案路径、模式与选项。
 */
public class DmpImportContext {

    private final ImportMode mode;
    private final DbConnectionProfile profile;
    private final String schemaName;
    private final String tableName;
    private final String dmpFilePath;
    private final String logFilePath;
    private final String remapSchema;
    private final DmpImportOptions options;
    private final Charset nativeCharset;

    private final String dmpToolPath;

    /**
     * @param mode          导入模式
     * @param profile       数据库连接（组 userid 字串）
     * @param schemaName    模式级时为模式名；表级时为表所属模式；整库可为空
     * @param tableName     表级时表名；多表建议以逗号分隔（依达梦工具约定）
     * @param dmpFilePath   DMP 档案完整路径
     * @param logFilePath   日志档完整路径
     * @param remapSchema   模式映射，如 SRC:TGT;SRC2:TGT2
     * @param options       导入选项
     * @param nativeCharset JNI 字串编码
     */
    public DmpImportContext(ImportMode mode, DbConnectionProfile profile, String schemaName, String tableName,
                            String dmpFilePath, String logFilePath, String remapSchema,
                            DmpImportOptions options, Charset nativeCharset, String dmpToolPath) {
        this.mode = Objects.requireNonNull(mode, "mode");
        this.profile = Objects.requireNonNull(profile, "profile");
        this.schemaName = schemaName == null ? "" : schemaName;
        this.tableName = tableName == null ? "" : tableName;
        this.dmpFilePath = Objects.requireNonNull(dmpFilePath, "dmpFilePath");
        this.logFilePath = Objects.requireNonNull(logFilePath, "logFilePath");
        this.remapSchema = remapSchema == null ? "" : remapSchema;
        this.options = options == null ? new DmpImportOptions() : options;
        this.nativeCharset = nativeCharset == null ? Charset.forName("GB18030") : nativeCharset;
        this.dmpToolPath = Objects.requireNonNull(dmpToolPath, "dmpToolPath");
    }

    public ImportMode getMode() {
        return mode;
    }

    public DbConnectionProfile getProfile() {
        return profile;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public String getTableName() {
        return tableName;
    }

    public String getDmpFilePath() {
        return dmpFilePath;
    }

    public String getLogFilePath() {
        return logFilePath;
    }

    public String getRemapSchema() {
        return remapSchema;
    }

    public DmpImportOptions getOptions() {
        return options;
    }

    public String getDmpToolPath() {
        return dmpToolPath;
    }
    public Charset getNativeCharset() {
        return nativeCharset;
    }

    /**
     * @return JNI userid 参数格式：使用者/密码@主机:端口
     */
    public String buildUseridString() {
        return profile.getUsername() + "/" + profile.getPassword()
                + "@" + profile.getHost() + ":" + profile.getPort();
    }
}
