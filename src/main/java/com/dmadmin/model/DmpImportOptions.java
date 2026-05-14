package com.dmadmin.model;

/**
 * DMP 导入行为选项：资料、索引、约束与已存在表处理等。
 * 具体是否生效取决于底层 dimp/JNI 支援，部分项仅作为日志与扩展预留。
 */
public class DmpImportOptions {

    private boolean ignoreExistingTables = true;
    private boolean importData = true;
    private boolean importIndexes = true;
    private boolean importConstraints = true;
    private boolean rollbackOnFailure = true;

    public boolean isIgnoreExistingTables() {
        return ignoreExistingTables;
    }

    public void setIgnoreExistingTables(boolean ignoreExistingTables) {
        this.ignoreExistingTables = ignoreExistingTables;
    }

    public boolean isImportData() {
        return importData;
    }

    public void setImportData(boolean importData) {
        this.importData = importData;
    }

    public boolean isImportIndexes() {
        return importIndexes;
    }

    public void setImportIndexes(boolean importIndexes) {
        this.importIndexes = importIndexes;
    }

    public boolean isImportConstraints() {
        return importConstraints;
    }

    public void setImportConstraints(boolean importConstraints) {
        this.importConstraints = importConstraints;
    }

    public boolean isRollbackOnFailure() {
        return rollbackOnFailure;
    }

    public void setRollbackOnFailure(boolean rollbackOnFailure) {
        this.rollbackOnFailure = rollbackOnFailure;
    }
}
