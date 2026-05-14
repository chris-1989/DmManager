package com.dmadmin.dao;

import com.dmadmin.exception.SqlExecutionException;
import com.dmadmin.exception.ValidationException;
import com.dmadmin.model.UserCreateRequest;
import com.dmadmin.model.UserSummary;
import com.dmadmin.util.SqlIdentifierValidator;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 使用者管理资料存取：建立、授权、查询、删除。
 */
public class UserManagementDao {

    private final DataSource dataSource;

    /**
     * @param dataSource 目标库资料来源（建议具 DBA 权限）
     */
    public UserManagementDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 建立使用者并可选授予 PUBLIC。
     *
     * @param request 建立请求
     * @throws SqlExecutionException SQL 失败
     */
    public void createUser(UserCreateRequest request) throws SqlExecutionException, ValidationException {
        SqlIdentifierValidator.requireSafeIdentifier(request.getUsername());
        SqlIdentifierValidator.requireSafeIdentifier(request.getDefaultTablespace());
        SqlIdentifierValidator.requireSafeIdentifier(request.getDefaultIndexTablespace());
        String u = SqlIdentifierValidator.quote(request.getUsername());
        String ts = SqlIdentifierValidator.quote(request.getDefaultTablespace());
        String its = SqlIdentifierValidator.quote(request.getDefaultIndexTablespace());
        String pwd = escapeQuote(request.getPassword());
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE USER ").append(u).append(" IDENTIFIED BY \"").append(pwd).append("\" ")
                .append("DEFAULT TABLESPACE ").append(ts).append(" DEFAULT INDEX TABLESPACE ").append(its);
        if (request.getProfileName() != null && !request.getProfileName().trim().isEmpty()) {
            SqlIdentifierValidator.requireSafeIdentifier(request.getProfileName());
            sql.append(" PROFILE ").append(SqlIdentifierValidator.quote(request.getProfileName()));
        }
        executeUpdate(sql.toString());
        if (request.isGrantPublicRole()) {
            grantRoles(request.getUsername(), listOf("PUBLIC"));
        }
    }

    /**
     * 授予系统角色（名称须通过识别符校验）。
     *
     * @param username 使用者
     * @param roles    角色名列表
     * @throws SqlExecutionException SQL 失败
     */
    public void grantRoles(String username, List<String> roles) throws SqlExecutionException, ValidationException {
        SqlIdentifierValidator.requireSafeIdentifier(username);
        if (roles == null || roles.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder("GRANT ");
        for (int i = 0; i < roles.size(); i++) {
            SqlIdentifierValidator.requireSafeIdentifier(roles.get(i));
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(SqlIdentifierValidator.quote(roles.get(i)));
        }
        sb.append(" TO ").append(SqlIdentifierValidator.quote(username));
        executeUpdate(sb.toString());
    }

    /**
     * 回收角色。
     *
     * @param username 使用者
     * @param roles    角色名列表
     * @throws SqlExecutionException SQL 失败
     */
    public void revokeRoles(String username, List<String> roles) throws SqlExecutionException, ValidationException {
        SqlIdentifierValidator.requireSafeIdentifier(username);
        if (roles == null || roles.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder("REVOKE ");
        for (int i = 0; i < roles.size(); i++) {
            SqlIdentifierValidator.requireSafeIdentifier(roles.get(i));
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(SqlIdentifierValidator.quote(roles.get(i)));
        }
        sb.append(" FROM ").append(SqlIdentifierValidator.quote(username));
        executeUpdate(sb.toString());
    }

    /**
     * 对象权限授予。
     *
     * @param schema   模式名
     * @param table    表名
     * @param username 被授权使用者
     * @param privs    如 SELECT、INSERT
     * @throws SqlExecutionException SQL 失败
     */
    public void grantTablePrivileges(String schema, String table, String username, List<String> privs)
            throws SqlExecutionException, ValidationException {
        SqlIdentifierValidator.requireSafeIdentifier(schema);
        SqlIdentifierValidator.requireSafeIdentifier(table);
        SqlIdentifierValidator.requireSafeIdentifier(username);
        if (privs == null || privs.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder("GRANT ");
        for (int i = 0; i < privs.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(normalizePrivilege(privs.get(i)));
        }
        sb.append(" ON ").append(SqlIdentifierValidator.quote(schema)).append('.')
                .append(SqlIdentifierValidator.quote(table))
                .append(" TO ").append(SqlIdentifierValidator.quote(username));
        executeUpdate(sb.toString());
    }

    /**
     * 回收对象权限。
     *
     * @param schema   模式名
     * @param table    表名
     * @param username 使用者
     * @param privs    权限列表
     * @throws SqlExecutionException SQL 失败
     */
    public void revokeTablePrivileges(String schema, String table, String username, List<String> privs)
            throws SqlExecutionException, ValidationException {
        SqlIdentifierValidator.requireSafeIdentifier(schema);
        SqlIdentifierValidator.requireSafeIdentifier(table);
        SqlIdentifierValidator.requireSafeIdentifier(username);
        if (privs == null || privs.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder("REVOKE ");
        for (int i = 0; i < privs.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(normalizePrivilege(privs.get(i)));
        }
        sb.append(" ON ").append(SqlIdentifierValidator.quote(schema)).append('.')
                .append(SqlIdentifierValidator.quote(table))
                .append(" FROM ").append(SqlIdentifierValidator.quote(username));
        executeUpdate(sb.toString());
    }

    /**
     * 模式 USAGE 授权（需数据库开启 GRANT_SCHEMA 等相关参数时方有效）。
     *
     * @param schema   模式名
     * @param username 使用者
     * @throws SqlExecutionException SQL 失败
     */
    public void grantSchemaUsage(String schema, String username) throws SqlExecutionException, ValidationException {
        SqlIdentifierValidator.requireSafeIdentifier(schema);
        SqlIdentifierValidator.requireSafeIdentifier(username);
        String sql = "GRANT USAGE ON SCHEMA " + SqlIdentifierValidator.quote(schema)
                + " TO " + SqlIdentifierValidator.quote(username);
        executeUpdate(sql);
    }

    /**
     * 回收模式 USAGE。
     *
     * @param schema   模式名
     * @param username 使用者
     * @throws SqlExecutionException SQL 失败
     */
    public void revokeSchemaUsage(String schema, String username) throws SqlExecutionException, ValidationException {
        SqlIdentifierValidator.requireSafeIdentifier(schema);
        SqlIdentifierValidator.requireSafeIdentifier(username);
        String sql = "REVOKE USAGE ON SCHEMA " + SqlIdentifierValidator.quote(schema)
                + " FROM " + SqlIdentifierValidator.quote(username);
        executeUpdate(sql);
    }

    /**
     * 查询使用者列表。
     *
     * @return 摘要清单
     * @throws SqlExecutionException SQL 失败
     */
    public List<UserSummary> listUsers() throws SqlExecutionException {
        String sql = "SELECT USERNAME, ACCOUNT_STATUS, DEFAULT_TABLESPACE, DEFAULT_INDEX_TABLESPACE "
                + "FROM DBA_USERS ORDER BY USERNAME";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<UserSummary> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new UserSummary(
                        rs.getString("USERNAME"),
                        rs.getString("ACCOUNT_STATUS"),
                        rs.getString("DEFAULT_TABLESPACE"),
                        rs.getString("DEFAULT_INDEX_TABLESPACE")));
            }
            return list;
        } catch (SQLException e) {
            throw new SqlExecutionException(sql, e);
        }
    }

    /**
     * 查询指定使用者的角色、系统权限、对象权限摘要行。
     *
     * @param username 使用者名
     * @return 人类可读权限行
     * @throws SqlExecutionException SQL 失败
     */
    public List<String> queryUserPrivileges(String username) throws SqlExecutionException, ValidationException {
        SqlIdentifierValidator.requireSafeIdentifier(username);
        Set<String> lines = new LinkedHashSet<>();
        appendRolePrivs(username, lines);
        appendSysPrivs(username, lines);
        appendTabPrivs(username, lines);
        return new ArrayList<>(lines);
    }

    /**
     * 删除使用者，可选 CASCADE。
     *
     * @param username 使用者名
     * @param cascade  是否级联删除其对象
     * @throws SqlExecutionException SQL 失败
     */
    public void dropUser(String username, boolean cascade) throws SqlExecutionException, ValidationException {
        SqlIdentifierValidator.requireSafeIdentifier(username);
        String sql = "DROP USER " + SqlIdentifierValidator.quote(username) + (cascade ? " CASCADE" : "");
        executeUpdate(sql);
    }

    private void appendRolePrivs(String username, Set<String> lines) throws SqlExecutionException {
        String sql = "SELECT GRANTED_ROLE FROM DBA_ROLE_PRIVS WHERE GRANTEE = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lines.add("ROLE: " + rs.getString(1));
                }
            }
        } catch (SQLException e) {
            throw new SqlExecutionException(sql, e);
        }
    }

    private void appendSysPrivs(String username, Set<String> lines) throws SqlExecutionException {
        String sql = "SELECT PRIVILEGE FROM DBA_SYS_PRIVS WHERE GRANTEE = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lines.add("SYS PRIV: " + rs.getString(1));
                }
            }
        } catch (SQLException e) {
            throw new SqlExecutionException(sql, e);
        }
    }

    private void appendTabPrivs(String username, Set<String> lines) throws SqlExecutionException {
        String sql = "SELECT OWNER, TABLE_NAME, PRIVILEGE FROM DBA_TAB_PRIVS WHERE GRANTEE = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lines.add("TABLE " + rs.getString(1) + "." + rs.getString(2) + " : " + rs.getString(3));
                }
            }
        } catch (SQLException e) {
            throw new SqlExecutionException(sql, e);
        }
    }

    private void executeUpdate(String sql) throws SqlExecutionException {
        try (Connection c = dataSource.getConnection();
             Statement st = c.createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException e) {
            throw new SqlExecutionException(sql, e);
        }
    }

    private static String escapeQuote(String password) {
        return password == null ? "" : password.replace("\"", "\"\"");
    }

    private static String normalizePrivilege(String p) {
        if (p == null || p.trim().isEmpty()) {
            throw new IllegalArgumentException("privilege empty");
        }
        String u = p.trim().toUpperCase();
        switch (u) {
            case "SELECT":
            case "INSERT":
            case "UPDATE":
            case "DELETE":
            case "ALTER":
            case "DROP":
            case "REFERENCES":
            case "INDEX":
                return u;
            default:
                throw new IllegalArgumentException("不支援的权限关键字: " + p);
        }
    }

    private static List<String> listOf(String a) {
        List<String> l = new ArrayList<>(1);
        l.add(a);
        return l;
    }
}
