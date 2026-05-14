package com.dmadmin.dmp;

/**
 * 工厂：依 {@link ImportMode} 建立对应导入处理器。
 */
public class DmpImportHandlerFactory {

    /**
     * @param mode 导入模式
     * @return 处理器实例
     */
    public DmpImportHandler create(ImportMode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("mode");
        }
        switch (mode) {
            case TABLE:
                return new TableDmpImportHandler();
            case SCHEMA:
                return new SchemaDmpImportHandler();
            case FULL_DATABASE:
                return new FullDatabaseDmpImportHandler();
            default:
                throw new IllegalArgumentException("不支援的模式: " + mode);
        }
    }
}
