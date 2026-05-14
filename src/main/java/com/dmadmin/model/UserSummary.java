package com.dmadmin.model;

import java.util.Objects;

/**
 * 使用者列表摘要资讯（对应 DBA_USERS 常用字段）。
 */
public class UserSummary {

    private final String username;
    private final String accountStatus;
    private final String defaultTablespace;
    private final String defaultIndexTablespace;

    /**
     * @param username               使用者名
     * @param accountStatus          账户状态
     * @param defaultTablespace      预设表空间
     * @param defaultIndexTablespace 预设索引表空间
     */
    public UserSummary(String username, String accountStatus, String defaultTablespace,
                       String defaultIndexTablespace) {
        this.username = Objects.requireNonNull(username, "username");
        this.accountStatus = accountStatus;
        this.defaultTablespace = defaultTablespace;
        this.defaultIndexTablespace = defaultIndexTablespace;
    }

    public String getUsername() {
        return username;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public String getDefaultTablespace() {
        return defaultTablespace;
    }

    public String getDefaultIndexTablespace() {
        return defaultIndexTablespace;
    }
}
