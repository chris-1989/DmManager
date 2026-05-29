package com.dmadmin.ui;

import com.dmadmin.exception.DmAdminException;
import com.dmadmin.model.DbConnectionProfile;
import com.dmadmin.service.ConnectionManagementService;
import com.dmadmin.util.SwingComponents;

import javax.swing.*;
import java.awt.*;

/**
 * 连接设定：注册 HikariCP 池并测试连接。
 */
public class ConnectionPanel extends JPanel {

    private final JTextField fieldId = new JTextField("default", 20);
    private final JTextField fieldHost = new JTextField("127.0.0.1", 20);
    private final JTextField fieldPort = new JTextField("5236", 8);
    private final JTextField fieldUser = new JTextField("SYSDBA", 20);
    private final JPasswordField fieldPass = new JPasswordField(20);
    private final ConnectionManagementService connService;
    private final SessionState session;
    private Runnable onConnected;
    private Runnable onDisconnected;

    /**
     * @param connService 连接服务
     * @param session     共用连接 ID
     */
    public ConnectionPanel(ConnectionManagementService connService, SessionState session) {
        this.connService = connService;
        this.session = session;
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        setLayout(new BorderLayout(0, 16));

        add(buildFormPanel(), BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);
    }

    /**
     * 注册连接成功后的回调（用于解锁其他功能页签）。
     */
    public void setOnConnected(Runnable callback) {
        this.onConnected = callback;
    }

    /**
     * 注册连接失败后的回调（用于重新锁定其他功能页签）。
     */
    public void setOnDisconnected(Runnable callback) {
        this.onDisconnected = callback;
    }

    private void fireConnected() {
        if (onConnected != null) {
            SwingUtilities.invokeLater(onConnected);
        }
    }

    private void fireDisconnected() {
        if (onDisconnected != null) {
            SwingUtilities.invokeLater(onDisconnected);
        }
    }

    private JPanel buildFormPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("数据库连接配置"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addRow(form, gbc, 0, "连接 ID", fieldId);
        addRow(form, gbc, 1, "主机", fieldHost);
        addRow(form, gbc, 2, "端口", fieldPort);
        addRow(form, gbc, 3, "使用者", fieldUser);
        addRow(form, gbc, 4, "密码", SwingComponents.createPasswordPanel(fieldPass));

        return form;
    }

    private void addRow(JPanel form, GridBagConstraints gbc, int row, String label, JComponent field) {
        SwingComponents.addFormRow(form, gbc, row, label, field);
    }

    private JPanel buildButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 8));

        JButton regBtn = new JButton("连接数据库");
        regBtn.addActionListener(e -> onRegister());

        JButton testBtn = new JButton("测试连接");
        testBtn.addActionListener(e -> onTest());

        panel.add(regBtn);
        panel.add(testBtn);
        return panel;
    }

    private void onRegister() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                DbConnectionProfile p = readProfile();
                connService.registerConnection(p);
                session.setConnectionId(p.getId());
                session.setConnectionProfile(p);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    msg("已注册连接池，当前连接 ID：" + session.getConnectionId(), false);
                    fireConnected();
                } catch (Exception ex) {
                    msg("注册失败：\n" + rootCause(ex), true);
                    fireDisconnected();
                }
            }
        }.execute();
    }

    private void onTest() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                DbConnectionProfile p = readProfile();
                connService.registerConnection(p);
                session.setConnectionId(p.getId());
                session.setConnectionProfile(p);
                connService.testConnection(p.getId());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    msg("连接测试成功。", false);
                    fireConnected();
                } catch (Exception ex) {
                    msg("连接测试失败：\n" + rootCause(ex), true);
                    fireDisconnected();
                }
            }
        }.execute();
    }

    private DbConnectionProfile readProfile() throws NumberFormatException {
        int port = Integer.parseInt(fieldPort.getText().trim());
        return new DbConnectionProfile(
                fieldId.getText().trim(),
                fieldHost.getText().trim(),
                port,
                fieldUser.getText().trim(),
                new String(fieldPass.getPassword()));
    }

    private void msg(String text, boolean error) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this, text, "连接",
                        error ? JOptionPane.ERROR_MESSAGE : JOptionPane.INFORMATION_MESSAGE));
    }

    private static String rootCause(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null && c.getCause() != c) {
            c = c.getCause();
        }
        if (c instanceof DmAdminException) {
            return c.getMessage();
        }
        return c.getMessage() != null ? c.getMessage() : c.getClass().getSimpleName();
    }
}
