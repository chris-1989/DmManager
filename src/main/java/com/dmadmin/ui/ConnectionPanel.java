package com.dmadmin.ui;

import com.dmadmin.exception.DmAdminException;
import com.dmadmin.model.DbConnectionProfile;
import com.dmadmin.service.ConnectionManagementService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

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
        addRow(form, gbc, 4, "密码", fieldPass);

        return form;
    }

    private void addRow(JPanel form, GridBagConstraints gbc, int row, String label, JTextField field) {
        JLabel jLabel = new JLabel(label);
        jLabel.setPreferredSize(new java.awt.Dimension(60, 28));
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        form.add(jLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weightx = 1;
        form.add(field, gbc);
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
                } catch (Exception ex) {
                    msg("注册失败：\n" + rootCause(ex), true);
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
                } catch (Exception ex) {
                    msg("连接测试失败：\n" + rootCause(ex), true);
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
