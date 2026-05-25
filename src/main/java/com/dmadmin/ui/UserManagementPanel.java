package com.dmadmin.ui;

import com.dmadmin.model.UserCreateRequest;
import com.dmadmin.model.UserSummary;
import com.dmadmin.pool.ConnectionPoolManager;
import com.dmadmin.service.UserManagementService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class UserManagementPanel extends JPanel {

    private final ConnectionPoolManager poolManager;
    private final SessionState session;
    private final DefaultTableModel userTableModel = new DefaultTableModel(
            new Object[]{"使用者", "预设表空间"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable userTable = new JTable(userTableModel);

    // 单用户创建组件
    private final JTextField newUser = new JTextField(15);
    private final JPasswordField newPass = new JPasswordField(15);
    private final JTextField newTs = new JTextField("MAIN", 15);
    private final JTextField newIdxTs = new JTextField(15);

    // 角色管理组件
    private final JTextField roleUser = new JTextField(20);
    private final JTextField rolesCsv = new JTextField("RESOURCE,DBA,PUBLIC,SOI,SVI,VTI", 28);
    private final JTextField dropUserField = new JTextField(20);
    private final JCheckBox dropCascade = new JCheckBox("CASCADE", true);
    private final JTextField privUser = new JTextField(20);
    private final JTextArea privOutput = new JTextArea(6, 40);

    // 批量创建组件
    private JComboBox<String> categoryCombo;
    private JTextArea batchOutputArea;
    private JTextArea previewUsersArea;
    private List<CategoryInfo> categories;

    // 批量角色授予组件
    private JComboBox<String> roleCategoryCombo;
    private JTextArea roleBatchOutputArea;

    public UserManagementPanel(ConnectionPoolManager poolManager, SessionState session) {
        this.poolManager = poolManager;
        this.session = session;
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // 左侧表格区域
        userTable.setRowHeight(24);
        userTable.getTableHeader().setFont(userTable.getTableHeader().getFont().deriveFont(Font.BOLD));
        JScrollPane tableScroll = new JScrollPane(userTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("数据库使用者列表"));
        tableScroll.setPreferredSize(new Dimension(280, 0));
        add(tableScroll, BorderLayout.CENTER);

        // 双击显示用户详情
        userTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = userTable.getSelectedRow();
                    if (row >= 0) {
                        String username = (String) userTableModel.getValueAt(row, 0);
                        showUserDetails(username);
                    }
                }
            }
        });

        // 自定义复制
        userTable.setTransferHandler(new TransferHandler() {
            @Override
            public void exportToClipboard(JComponent comp, Clipboard clip, int action) {
                int selectedRow = userTable.getSelectedRow();
                if (selectedRow >= 0) {
                    Object value = userTableModel.getValueAt(selectedRow, 0);
                    String username = value == null ? "" : value.toString().trim();
                    if (!username.isEmpty()) {
                        StringSelection selection = new StringSelection(username);
                        clip.setContents(selection, null);
                    }
                }
            }
            @Override
            public boolean canImport(TransferSupport support) {
                return false;
            }
        });

        // 右侧控制面板
        JPanel rightPanel = buildRightPanel();
        JScrollPane controlScroll = new JScrollPane(rightPanel);
        controlScroll.setBorder(BorderFactory.createEmptyBorder());
        controlScroll.setPreferredSize(new Dimension(640, 0));
        controlScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        controlScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        controlScroll.getVerticalScrollBar().setUnitIncrement(16);
        add(controlScroll, BorderLayout.EAST);

        privOutput.setEditable(false);
        loadCategoriesFromProperties();
        initBatchCreationUI();
        initRoleBatchUI();
    }

    private void showUserDetails(String username) {
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                List<String> lines = svc().queryUserPrivileges(username);
                return lines.stream().collect(Collectors.joining("\n"));
            }
            @Override
            protected void done() {
                try {
                    String details = get();
                    JTextArea textArea = new JTextArea(details);
                    textArea.setEditable(false);
                    textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
                    JScrollPane scrollPane = new JScrollPane(textArea);
                    scrollPane.setPreferredSize(new Dimension(620, 420));
                    JOptionPane.showMessageDialog(UserManagementPanel.this,
                            scrollPane,
                            "使用者详情: " + username,
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    alert(ex);
                }
            }
        }.execute();
    }

    private JPanel buildRightPanel() {
        ScrollablePanel panel = new ScrollablePanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 16, 20, 8));

        // 刷新按钮
        JButton refreshBtn = new JButton("刷新使用者列表");
        refreshBtn.addActionListener(e -> refreshUsers());
        JPanel refreshPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        refreshPanel.add(refreshBtn);
        panel.add(refreshPanel);
        panel.add(Box.createVerticalStrut(16));

        // 各功能区块
        panel.add(createSection("建立使用者", buildCreateUserCombinedPanel()));
        panel.add(Box.createVerticalStrut(14));
        panel.add(createSection("角色管理", buildRolePanel()));
        panel.add(Box.createVerticalStrut(14));
        panel.add(createSection("删除使用者", buildDropPanel()));
        panel.add(Box.createVerticalStrut(14));
        panel.add(createSection("权限查询", buildPrivilegePanel()));

        return panel;
    }

    private JPanel buildCreateUserCombinedPanel() {
        JPanel p = new JPanel(new BorderLayout(5, 10));

        // 上半部分：单用户创建
        JPanel singlePanel = new JPanel(new GridBagLayout());
        singlePanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 12);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; singlePanel.add(new JLabel("名称:"), gbc);
        gbc.gridx = 1; singlePanel.add(newUser, gbc);
        gbc.gridx = 2; singlePanel.add(new JLabel("密码:"), gbc);
        gbc.gridx = 3; singlePanel.add(newPass, gbc);

        gbc.gridx = 0; gbc.gridy = 1; singlePanel.add(new JLabel("预设表空间:"), gbc);
        gbc.gridx = 1; singlePanel.add(newTs, gbc);
        gbc.gridx = 2; singlePanel.add(new JLabel("索引表空间:"), gbc);
        gbc.gridx = 3; singlePanel.add(newIdxTs, gbc);

        JButton createBtn = new JButton("建立单用户");
        createBtn.addActionListener(e -> createUser());
        gbc.gridx = 3; gbc.gridy = 2; singlePanel.add(createBtn, gbc);

        // 下半部分：批量创建
        JPanel batchPanel = new JPanel(new BorderLayout(5, 5));
        batchPanel.setBorder(BorderFactory.createTitledBorder("批量创建用户（基于配置文件）"));
        JPanel batchTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        batchTop.add(new JLabel("使用者类别:"));
        categoryCombo = new JComboBox<>();
        batchTop.add(categoryCombo);
        JButton batchCreateBtn = new JButton("批量创建");
        batchCreateBtn.addActionListener(e -> batchCreateUsers());
        batchTop.add(batchCreateBtn);
        batchPanel.add(batchTop, BorderLayout.NORTH);

        previewUsersArea = new JTextArea(6, 40);
        previewUsersArea.setEditable(false);
        previewUsersArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane previewScroll = new JScrollPane(previewUsersArea);
        previewScroll.setBorder(BorderFactory.createTitledBorder("当前类别下的用户列表"));
        batchPanel.add(previewScroll, BorderLayout.CENTER);

        batchOutputArea = new JTextArea(10, 40);
        batchOutputArea.setEditable(false);
        batchOutputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(batchOutputArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("批量创建日志"));
        batchPanel.add(logScroll, BorderLayout.SOUTH);

        p.add(singlePanel, BorderLayout.NORTH);
        p.add(batchPanel, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildRolePanel() {
        JPanel p = new JPanel(new BorderLayout(5, 10));

        // 上半部分：单个用户授权/回收
        JPanel singlePanel = new JPanel(new BorderLayout(5, 5));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        top.add(new JLabel("使用者:"));
        top.add(roleUser);
        top.add(new JLabel("角色(逗号分隔):"));
        top.add(rolesCsv);
        singlePanel.add(top, BorderLayout.NORTH);
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton grant = new JButton("授予角色");
        grant.addActionListener(e -> grantRoles());
        JButton revoke = new JButton("回收角色");
        revoke.addActionListener(e -> revokeRoles());
        btnPanel.add(grant);
        btnPanel.add(revoke);
        singlePanel.add(btnPanel, BorderLayout.CENTER);

        // 下半部分：批量授予角色
        JPanel batchPanel = new JPanel(new BorderLayout(5, 5));
        batchPanel.setBorder(BorderFactory.createTitledBorder("批量授予角色（按用户类别）"));
        JPanel batchTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        batchTop.add(new JLabel("用户类别:"));
        roleCategoryCombo = new JComboBox<>();
        batchTop.add(roleCategoryCombo);
        JButton batchGrantBtn = new JButton("批量授予角色");
        batchGrantBtn.addActionListener(e -> batchGrantRoles());
        batchTop.add(batchGrantBtn);
        batchPanel.add(batchTop, BorderLayout.NORTH);

        roleBatchOutputArea = new JTextArea(8, 40);
        roleBatchOutputArea.setEditable(false);
        roleBatchOutputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(roleBatchOutputArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("批量授权日志"));
        batchPanel.add(logScroll, BorderLayout.CENTER);

        p.add(singlePanel, BorderLayout.NORTH);
        p.add(batchPanel, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildDropPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        p.add(new JLabel("使用者:"));
        p.add(dropUserField);
        p.add(dropCascade);
        JButton dropBtn = new JButton("删除使用者");
        dropBtn.addActionListener(e -> dropUser());
        p.add(dropBtn);
        return p;
    }

    private JPanel buildPrivilegePanel() {
        JPanel p = new JPanel(new BorderLayout(5, 5));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        top.add(new JLabel("使用者:"));
        top.add(privUser);
        JButton queryBtn = new JButton("查询权限");
        queryBtn.addActionListener(e -> queryPrivs());
        top.add(queryBtn);
        p.add(top, BorderLayout.NORTH);
        JScrollPane privScroll = new JScrollPane(privOutput);
        privOutput.setFont(new Font("Monospaced", Font.PLAIN, 12));
        privScroll.setPreferredSize(new Dimension(580, 120));
        p.add(privScroll, BorderLayout.CENTER);
        return p;
    }

    private JPanel createSection(String title, JComponent content) {
        JPanel section = new JPanel(new BorderLayout());
        section.setBorder(BorderFactory.createTitledBorder(title));
        section.add(content, BorderLayout.CENTER);
        return section;
    }

    // ---------- 批量创建用户相关逻辑 ----------
    private void loadCategoriesFromProperties() {
        Properties props = new Properties();
        try (InputStream is = getClass().getResourceAsStream("/application.properties");
             InputStreamReader reader = is != null ? new InputStreamReader(is, StandardCharsets.UTF_8) : null) {
            if (reader != null) props.load(reader);
        } catch (IOException e) { e.printStackTrace(); }

        Path localPath = Paths.get("dm-admin-local.properties");
        if (Files.exists(localPath)) {
            try (InputStream is = Files.newInputStream(localPath);
                 InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                props.load(reader);
            } catch (IOException e) { e.printStackTrace(); }
        }

        categories = new ArrayList<>();
        String categoryKeysRaw = props.getProperty("dm.user.categories");
        if (categoryKeysRaw == null || categoryKeysRaw.trim().isEmpty()) {
            if (categoryCombo != null) categoryCombo.setEnabled(false);
            if (roleCategoryCombo != null) roleCategoryCombo.setEnabled(false);
            if (previewUsersArea != null) previewUsersArea.setText("未配置任何类别");
            return;
        }
        String[] keys = categoryKeysRaw.split(",");
        for (String key : keys) {
            key = key.trim();
            String name = props.getProperty("dm.user.category." + key + ".name", key);
            String usersRaw = props.getProperty("dm.user.category." + key + ".users");
            if (usersRaw == null || usersRaw.trim().isEmpty()) continue;
            List<String> users = Arrays.stream(usersRaw.split(","))
                    .map(String::trim).filter(u -> !u.isEmpty()).collect(Collectors.toList());
            if (users.isEmpty()) continue;

            String password = props.getProperty("dm.user.category." + key + ".password");
            String tablespace = props.getProperty("dm.user.category." + key + ".tablespace");
            String indexts = props.getProperty("dm.user.category." + key + ".indexts");
            categories.add(new CategoryInfo(name, users, password, tablespace, indexts));
        }

        if (categories.isEmpty()) {
            if (categoryCombo != null) categoryCombo.setEnabled(false);
            if (roleCategoryCombo != null) roleCategoryCombo.setEnabled(false);
            if (previewUsersArea != null) previewUsersArea.setText("未找到有效用户类别");
        } else {
            if (categoryCombo != null) {
                categoryCombo.setEnabled(true);
                for (CategoryInfo cat : categories) categoryCombo.addItem(cat.getDisplayName());
            }
            if (roleCategoryCombo != null) {
                roleCategoryCombo.setEnabled(true);
                for (CategoryInfo cat : categories) roleCategoryCombo.addItem(cat.getDisplayName());
            }
        }
    }

    private void initBatchCreationUI() {
        if (categoryCombo == null) return;
        categoryCombo.addActionListener(e -> {
            int idx = categoryCombo.getSelectedIndex();
            if (idx >= 0 && idx < categories.size()) {
                previewUsersArea.setText(String.join("\n", categories.get(idx).getUsers()));
            } else previewUsersArea.setText("");
        });
        if (categories != null && !categories.isEmpty()) categoryCombo.setSelectedIndex(0);
    }

    private void initRoleBatchUI() {
    }

    private void batchGrantRoles() {
        int idx = roleCategoryCombo.getSelectedIndex();
        if (idx < 0 || idx >= categories.size()) {
            alertInfo("请先选择一个用户类别");
            return;
        }
        CategoryInfo cat = categories.get(idx);
        List<String> users = cat.getUsers();
        if (users.isEmpty()) {
            alertInfo("当前类别下没有用户");
            return;
        }

        List<String> roles = splitCsv(rolesCsv.getText());
        if (roles.isEmpty()) {
            alertInfo("请填写要授予的角色（逗号分隔）");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                String.format("即将为 %d 个用户授予角色: %s\n是否继续？", users.size(), String.join(",", roles)),
                "批量授权确认", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        roleBatchOutputArea.setText("");
        appendRoleBatchLog("========== 开始批量授予角色 (类别: " + cat.getDisplayName() + ") ==========\n");
        appendRoleBatchLog("授予角色: " + String.join(", ", roles) + "\n");

        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                for (String username : users) {
                    try {
                        svc().grantRoles(username, roles);
                        publish("[成功] 用户 " + username + " 授予角色成功。\n");
                    } catch (Exception ex) {
                        String errMsg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
                        publish("[失败] " + username + " : " + errMsg + "\n");
                    }
                }
                publish("\n========== 批量授权执行完毕 ==========");
                return null;
            }
            @Override
            protected void process(List<String> chunks) {
                for (String msg : chunks) appendRoleBatchLog(msg);
            }
            @Override
            protected void done() {
                try { get(); } catch (Exception ex) {
                    appendRoleBatchLog("\n[系统错误] " + ex.getMessage());
                } finally {
                    appendRoleBatchLog("\n授权完成，请手动刷新用户列表查看角色变更。");
                }
            }
        }.execute();
    }

    private void appendRoleBatchLog(String msg) {
        SwingUtilities.invokeLater(() -> {
            roleBatchOutputArea.append(msg);
            roleBatchOutputArea.setCaretPosition(roleBatchOutputArea.getDocument().getLength());
        });
    }

    private void batchCreateUsers() {
        int idx = categoryCombo.getSelectedIndex();
        if (idx < 0 || idx >= categories.size()) {
            alertInfo("请先选择一个使用者类别");
            return;
        }
        CategoryInfo cat = categories.get(idx);
        List<String> users = cat.getUsers();
        if (users.isEmpty()) {
            alertInfo("当前类别下没有用户需要创建");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                String.format("即将批量创建 %d 个用户 (类别: %s)\n是否继续？", users.size(), cat.getDisplayName()),
                "批量创建确认", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        batchOutputArea.setText("");
        appendBatchLog("========== 开始批量创建用户 (类别: " + cat.getDisplayName() + ") ==========\n");

        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                for (String username : users) {
                    try {
                        UserCreateRequest.Builder builder = UserCreateRequest.builder()
                                .username(username)
                                .password(resolvePassword(cat))
                                .defaultTablespace(resolveTablespace(cat))
                                .grantPublicRole(false);
                        String idxTs = resolveIndexTablespace(cat);
                        if (idxTs != null && !idxTs.trim().isEmpty()) {
                            builder.defaultIndexTablespace(idxTs);
                        }
                        svc().createUser(builder.build());
                        publish("[成功] 用户 " + username + " 创建成功。\n");
                    } catch (Exception ex) {
                        String errMsg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
                        publish("[失败] " + username + " : " + errMsg + "\n");
                    }
                }
                publish("\n========== 批量创建执行完毕 ==========");
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String msg : chunks) appendBatchLog(msg);
            }

            @Override
            protected void done() {
                try { get(); } catch (Exception ex) {
                    appendBatchLog("\n[系统错误] " + ex.getMessage());
                } finally {
                    appendBatchLog("\n请手动点击「刷新使用者列表」按钮查看最新结果。");
                    refreshUsers();
                }
            }
        }.execute();
    }

    private String resolvePassword(CategoryInfo cat) {
        if (cat.getPassword() != null && !cat.getPassword().trim().isEmpty()) return cat.getPassword();
        return new String(newPass.getPassword());
    }

    private String resolveTablespace(CategoryInfo cat) {
        if (cat.getTablespace() != null && !cat.getTablespace().trim().isEmpty()) return cat.getTablespace();
        return newTs.getText().trim();
    }

    private String resolveIndexTablespace(CategoryInfo cat) {
        if (cat.getIndexTablespace() != null && !cat.getIndexTablespace().trim().isEmpty()) return cat.getIndexTablespace();
        return newIdxTs.getText().trim();
    }

    private void appendBatchLog(String msg) {
        SwingUtilities.invokeLater(() -> {
            batchOutputArea.append(msg);
            batchOutputArea.setCaretPosition(batchOutputArea.getDocument().getLength());
        });
    }

    // ---------- 业务方法 ----------
    private void requireSession() throws IllegalStateException {
        if (!session.hasConnection()) {
            throw new IllegalStateException("请先在《连接》分页中配置用户名密码，连接数据库。");
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
                                u.getDefaultTablespace()
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
                if (!idx.isEmpty()) b.defaultIndexTablespace(idx);
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

    private static class ScrollablePanel extends JPanel implements Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }
        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }
        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 48;
        }
        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }
        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private static class CategoryInfo {
        private final String displayName;
        private final List<String> users;
        private final String password;
        private final String tablespace;
        private final String indexTablespace;
        public CategoryInfo(String displayName, List<String> users, String password, String tablespace, String indexTablespace) {
            this.displayName = displayName;
            this.users = users;
            this.password = password;
            this.tablespace = tablespace;
            this.indexTablespace = indexTablespace;
        }
        public String getDisplayName() { return displayName; }
        public List<String> getUsers() { return users; }
        public String getPassword() { return password; }
        public String getTablespace() { return tablespace; }
        public String getIndexTablespace() { return indexTablespace; }
    }
}
