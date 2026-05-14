package com.dmadmin.model;

import java.util.Objects;

/**
 * 建立使用者请求：账号、密码、表空间与可选密码策略。
 */
public class UserCreateRequest {

    private final String username;
    private final String password;
    private final String defaultTablespace;
    private final String defaultIndexTablespace;
    private final Integer passwordLifeTimeDays;
    private final Integer failedLoginAttempts;
    private final boolean grantPublicRole;
    private final String profileName;

    private UserCreateRequest(Builder b) {
        this.username = Objects.requireNonNull(b.username, "username");
        this.password = Objects.requireNonNull(b.password, "password");
        this.defaultTablespace = Objects.requireNonNull(b.defaultTablespace, "defaultTablespace");
        this.defaultIndexTablespace =
                b.defaultIndexTablespace != null ? b.defaultIndexTablespace : b.defaultTablespace;
        this.passwordLifeTimeDays = b.passwordLifeTimeDays;
        this.failedLoginAttempts = b.failedLoginAttempts;
        this.grantPublicRole = b.grantPublicRole;
        this.profileName = b.profileName;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getDefaultTablespace() {
        return defaultTablespace;
    }

    public String getDefaultIndexTablespace() {
        return defaultIndexTablespace;
    }

    public Integer getPasswordLifeTimeDays() {
        return passwordLifeTimeDays;
    }

    public Integer getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public boolean isGrantPublicRole() {
        return grantPublicRole;
    }

    public String getProfileName() {
        return profileName;
    }

    /**
     * @return 建立请求的建造器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * {@link UserCreateRequest} 建造器。
     */
    public static final class Builder {
        private String username;
        private String password;
        private String defaultTablespace;
        private String defaultIndexTablespace;
        private Integer passwordLifeTimeDays;
        private Integer failedLoginAttempts;
        private boolean grantPublicRole = true;
        private String profileName;

        private Builder() {
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder defaultTablespace(String defaultTablespace) {
            this.defaultTablespace = defaultTablespace;
            return this;
        }

        public Builder defaultIndexTablespace(String defaultIndexTablespace) {
            this.defaultIndexTablespace = defaultIndexTablespace;
            return this;
        }

        public Builder passwordLifeTimeDays(Integer passwordLifeTimeDays) {
            this.passwordLifeTimeDays = passwordLifeTimeDays;
            return this;
        }

        public Builder failedLoginAttempts(Integer failedLoginAttempts) {
            this.failedLoginAttempts = failedLoginAttempts;
            return this;
        }

        public Builder grantPublicRole(boolean grantPublicRole) {
            this.grantPublicRole = grantPublicRole;
            return this;
        }

        public Builder profileName(String profileName) {
            this.profileName = profileName;
            return this;
        }

        public UserCreateRequest build() {
            return new UserCreateRequest(this);
        }
    }
}
