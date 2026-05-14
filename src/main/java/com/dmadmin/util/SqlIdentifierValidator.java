package com.dmadmin.util;

import com.dmadmin.exception.ValidationException;

import java.util.regex.Pattern;

/**
 * SQL 识别符白名单校验，降低动态拼接 SQL 的注入风险。
 * 仅允许字母、数字、底线，且首字元为字母或底线。
 */
public final class SqlIdentifierValidator {

    private static final Pattern SAFE = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]{0,127}$");

    private SqlIdentifierValidator() {
    }

    /**
     * 校验识别符是否可用于双引号外的标准命名，或作为双引号内简单名称。
     *
     * @param name 使用者名、模式名、表名等
     * @throws ValidationException 不合法时抛出
     */
    public static void requireSafeIdentifier(String name) throws ValidationException {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("识别符不可为空");
        }
        if (!SAFE.matcher(name).matches()) {
            throw new ValidationException("识别符仅允许字母、数字、底线，且长度不超过 128");
        }
    }

    /**
     * 将识别符以双引号包裹（达梦区分大小写建议）。
     *
     * @param name 已通过 {@link #requireSafeIdentifier(String)} 的名称
     * @return 例如 {@code "MYUSER"}
     */
    public static String quote(String name) {
        return "\"" + name.replace("\"", "\"\"") + "\"";
    }
}
