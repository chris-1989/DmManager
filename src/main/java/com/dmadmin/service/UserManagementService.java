package com.dmadmin.service;

import com.dmadmin.dao.UserManagementDao;
import com.dmadmin.exception.ConnectionFailureException;
import com.dmadmin.exception.DmAdminException;
import com.dmadmin.exception.SqlExecutionException;
import com.dmadmin.exception.ValidationException;
import com.dmadmin.model.UserCreateRequest;
import com.dmadmin.model.UserSummary;
import com.dmadmin.pool.ConnectionPoolManager;
import com.dmadmin.util.PasswordStrengthValidator;

import java.util.List;

/**
 * 使用者管理服务：校验密码强度并委派 DAO。
 */
public class UserManagementService {

    private final String connectionId;
    private final ConnectionPoolManager poolManager;

    /**
     * @param connectionId 已注册的连接池 ID
     * @param poolManager  连接池单例
     */
    public UserManagementService(String connectionId, ConnectionPoolManager poolManager) {
        this.connectionId = connectionId;
        this.poolManager = poolManager;
    }

    private UserManagementDao dao() throws ConnectionFailureException {
        return new UserManagementDao(poolManager.getDataSource(connectionId));
    }

    /**
     * 建立使用者（含密码强度校验）。
     *
     * @param request 建立请求
     * @throws ValidationException        校验失败
     * @throws ConnectionFailureException 无法取得连接
     * @throws SqlExecutionException      SQL 失败
     */
    public void createUser(UserCreateRequest request)
            throws ValidationException, ConnectionFailureException, SqlExecutionException {
        PasswordStrengthValidator.validate(request.getPassword());
        dao().createUser(request);
    }

    /**
     * 授予角色。
     *
     * @param username 使用者
     * @param roles    角色名
     * @throws DmAdminException 底层异常
     */
    public void grantRoles(String username, List<String> roles) throws DmAdminException {
        dao().grantRoles(username, roles);
    }

    /**
     * 回收角色。
     *
     * @param username 使用者
     * @param roles    角色名
     * @throws DmAdminException 底层异常
     */
    public void revokeRoles(String username, List<String> roles) throws DmAdminException {
        dao().revokeRoles(username, roles);
    }

    /**
     * 授予表上对象权限。
     *
     * @param schema   模式
     * @param table    表
     * @param username 使用者
     * @param privs    权限清单
     * @throws DmAdminException 底层异常
     */
    public void grantTablePrivileges(String schema, String table, String username, List<String> privs)
            throws DmAdminException {
        dao().grantTablePrivileges(schema, table, username, privs);
    }

    /**
     * 回收表上对象权限。
     *
     * @param schema   模式
     * @param table    表
     * @param username 使用者
     * @param privs    权限清单
     * @throws DmAdminException 底层异常
     */
    public void revokeTablePrivileges(String schema, String table, String username, List<String> privs)
            throws DmAdminException {
        dao().revokeTablePrivileges(schema, table, username, privs);
    }

    /**
     * 授予模式 USAGE。
     *
     * @param schema   模式
     * @param username 使用者
     * @throws DmAdminException 底层异常
     */
    public void grantSchemaUsage(String schema, String username) throws DmAdminException {
        dao().grantSchemaUsage(schema, username);
    }

    /**
     * 回收模式 USAGE。
     *
     * @param schema   模式
     * @param username 使用者
     * @throws DmAdminException 底层异常
     */
    public void revokeSchemaUsage(String schema, String username) throws DmAdminException {
        dao().revokeSchemaUsage(schema, username);
    }

    /**
     * 列出使用者。
     *
     * @return 摘要清单
     * @throws DmAdminException 底层异常
     */
    public List<UserSummary> listUsers() throws DmAdminException {
        return dao().listUsers();
    }

    /**
     * 查询使用者权限文字行。
     *
     * @param username 使用者
     * @return 权限描述行
     * @throws DmAdminException 底层异常
     */
    public List<String> queryUserPrivileges(String username) throws DmAdminException {
        return dao().queryUserPrivileges(username);
    }

    /**
     * 删除使用者。
     *
     * @param username 使用者
     * @param cascade  是否 CASCADE
     * @throws DmAdminException 底层异常
     */
    public void dropUser(String username, boolean cascade) throws DmAdminException {
        dao().dropUser(username, cascade);
    }
}
