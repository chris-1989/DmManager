package com.dmadmin.ui;

import com.dmadmin.model.UserCreateRequest;
import com.dmadmin.model.UserSummary;
import com.dmadmin.pool.ConnectionPoolManager;
import com.dmadmin.service.UserManagementService;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 使用者管理：列表、建立、角色授权、删除、权限查询。
 */
public class UserManagementPanel extends JPanel {

    private final ConnectionPoolManager poolManager;
    private final SessionState session;
    private final DefaultTableModel userTableModel = new DefaultTableModel(
            new Object[]{"使用者", "状态", "预设表空间", "预设索引表空间"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable userTable = new JTable(userTableModel);
    private final JTextField newUser = new JTextField(20);
    private final JPasswordField newPass = new JPasswordField(20);
    private final JTextField newTs = new JTextField("MAIN", 20);
    private final JTextField newIdxTs = new JTextField(20);
    private final JTextField roleUser = new JTextField(30);
    private final JTextField rolesCsv = new JTextField("RESOURCE,DBA,PUBLIC,SOI,SVI,VTI", 50);
    private final JTextField dropUserField = new JTextField(30);
    private final JCheckBox dropCascade = new JCheckBox("CASCADE", true);
    private final JTextField privUser = new JTextField(30);
    private final JTextArea privOutput = new JTextArea(8, 40);

    /**
     * @param poolManager 连接池
     * @param session     当前连接 ID
     */
    public UserManagementPanel(ConnectionPoolManager poolManager, SessionState session) {
        this.poolManager = poolManager;
        this.session = session;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(new JScrollPane(userTable), BorderLayout.CENTER);
        add(buildSouth(), BorderLayout.SOUTH);
        privOutput.setEditable(false);
    }

    private JPanel buildSouth() {
        JPanel p = new JPanel(new BorderLayout());
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refresh = new JButton("重新整理使用者列表");
        refresh.addActionListener(e -> refreshUsers());
        top.add(refresh);
        p.add(top, BorderLayout.NORTH);
        JPanel grid = new JPanel();
        grid.setLayout(new javax.swing.BoxLayout(grid, javax.swing.BoxLayout.Y_AXIS));
        grid.add(row("建立使用者：名称", newUser, new JLabel("密码"), newPass));
        grid.add(row("预设表空间", newTs, new JLabel("索引表空间(可空)"), newIdxTs));
        JButton create = new JButton("建立");
        create.addActionListener(e -> createUser());
        grid.add(flow(create));
        grid.add(Box.createVerticalStrut(8));
        grid.add(row("角色：使用者", roleUser, new JLabel("角色(逗号分隔)"), rolesCsv));
        JButton grant = new JButton("授予角色");
        grant.addActionListener(e -> grantRoles());
        JButton revoke = new JButton("回收角色");
        revoke.addActionListener(e -> revokeRoles());
        grid.add(flow(grant, revoke));
        grid.add(Box.createVerticalStrut(8));
        grid.add(row("删除使用者", dropUserField, new JLabel(""), new JLabel("")));
        grid.add(flow(dropCascade, button("删除使用者", this::dropUser)));
        grid.add(Box.createVerticalStrut(8));
        grid.add(row("查询权限：使用者", privUser, new JLabel(""), new JLabel("")));
        grid.add(flow(button("查询权限", this::queryPrivs)));
        grid.add(new JScrollPane(privOutput));
        p.add(grid, BorderLayout.CENTER);
        return p;
    }

    private static JPanel row(String l1, JComponent f1, JComponent l2, JComponent f2) {
        JPanel r = new JPanel(new FlowLayout(FlowLayout.LEFT));
        r.add(new JLabel(l1));
        r.add(f1);
        r.add(l2);
        r.add(f2);
        return r;
    }

    private static JPanel flow(JComponent... c) {
        JPanel r = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (JComponent x : c) {
            r.add(x);
        }
        return r;
    }

    private static JButton button(String t, Runnable a) {
        JButton b = new JButton(t);
        b.addActionListener(e -> a.run());
        return b;
    }

    private void requireSession() throws IllegalStateException {
        if (!session.hasConnection()) {
            throw new IllegalStateException("请先在“连接”分页注册并测试连接。");
        }
    }

    private UserManagementService svc() throws IllegalStateException {
        requireSession();
        return new UserManagementService(session.getConnectionId(), poolManager);
    }

    private void refreshUsers() {
        new SwingWorker<List<UserSummary>, Void>() {
            @Override
            protected List<UserSummary> doInBackground() throws Exception {
                return svc().listUsers();
            }

            @Override
            protected void done() {
                try {
                    List<UserSummary> rows = get();
                    userTableModel.setRowCount(0);
                    for (UserSummary u : rows) {
                        userTableModel.addRow(new Object[]{
                                u.getUsername(),
                                u.getAccountStatus(),
                                u.getDefaultTablespace(),
                                u.getDefaultIndexTablespace()
                        });
                    }
                } catch (Exception ex) {
                    alert(ex);
                }
            }
        }.execute();
    }

    private void createUser() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                UserCreateRequest.Builder b = UserCreateRequest.builder()
                        .username(newUser.getText().trim())
                        .password(new String(newPass.getPassword()))
                        .defaultTablespace(newTs.getText().trim());
                String idx = newIdxTs.getText().trim();
                if (!idx.isEmpty()) {
                    b.defaultIndexTablespace(idx);
                }
                svc().createUser(b.build());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    alertInfo("建立使用者成功。");
                    refreshUsers();
                } catch (Exception ex) {
                    alert(ex);
                }
            }
        }.execute();
    }

    private void grantRoles() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                List<String> roles = splitCsv(rolesCsv.getText());
                svc().grantRoles(roleUser.getText().trim(), roles);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    alertInfo("授予角色完成。");
                } catch (Exception ex) {
                    alert(ex);
                }
            }
        }.execute();
    }

    private void revokeRoles() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                List<String> roles = splitCsv(rolesCsv.getText());
                svc().revokeRoles(roleUser.getText().trim(), roles);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    alertInfo("回收角色完成。");
                } catch (Exception ex) {
                    alert(ex);
                }
            }
        }.execute();
    }

    private void dropUser() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                svc().dropUser(dropUserField.getText().trim(), dropCascade.isSelected());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    alertInfo("已删除使用者。");
                    refreshUsers();
                } catch (Exception ex) {
                    alert(ex);
                }
            }
        }.execute();
    }

    private void queryPrivs() {
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                List<String> lines = svc().queryUserPrivileges(privUser.getText().trim());
                return lines.stream().collect(Collectors.joining("\n"));
            }

            @Override
            protected void done() {
                try {
                    privOutput.setText(get());
                } catch (Exception ex) {
                    alert(ex);
                }
            }
        }.execute();
    }

    private static List<String> splitCsv(String s) {
        return Arrays.stream(s.split(","))
                .map(String::trim)
                .filter(x -> !x.isEmpty())
                .collect(Collectors.toList());
    }

    private void alert(Exception ex) {
        Throwable t = ex;
        if (ex instanceof java.util.concurrent.ExecutionException && ex.getCause() != null) {
            t = ex.getCause();
        }
        String msg = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this, msg, "错误", JOptionPane.ERROR_MESSAGE));
    }

    private void alertInfo(String msg) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this, msg, "提示", JOptionPane.INFORMATION_MESSAGE));
    }
}
