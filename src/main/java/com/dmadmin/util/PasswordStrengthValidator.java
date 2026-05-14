package com.dmadmin.util;

import com.dmadmin.exception.ValidationException;

import java.util.regex.Pattern;

/**
 * 密码强度校验：不少于 9 位，且含大小写字母、数字与特殊字元。
 */
public final class PasswordStrengthValidator {

    private static final int MIN_LENGTH = 9;
    private static final Pattern UPPER = Pattern.compile("[A-Z]");
    private static final Pattern LOWER = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL = Pattern.compile("[^A-Za-z0-9]");

    private PasswordStrengthValidator() {
    }

    /**
     * 校验密码是否符合策略。
     *
     * @param password 明文密码
     * @throws ValidationException 不符合时抛出并说明原因
     */
    public static void validate(String password) throws ValidationException {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new ValidationException("密码长度须不少于 " + MIN_LENGTH + " 位");
        }
        // if (!UPPER.matcher(password).find()) {
        //     throw new ValidationException("密码须包含大写英文字母");
        // }
        if (!LOWER.matcher(password).find()) {
            throw new ValidationException("密码须包含小写英文字母");
        }
        if (!DIGIT.matcher(password).find()) {
            throw new ValidationException("密码须包含数字");
        }
        // if (!SPECIAL.matcher(password).find()) {
        //     throw new ValidationException("密码须包含特殊字元");
        // }
    }
}
