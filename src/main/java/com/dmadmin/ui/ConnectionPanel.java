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
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * 连接设定：注册 HikariCP 池并测试连接。
 */
public class ConnectionPanel extends JPanel {

    private final JTextField fieldId = new JTextField("default", 16);
    private final JTextField fieldHost = new JTextField("127.0.0.1", 16);
    private final JTextField fieldPort = new JTextField("5236", 8);
    private final JTextField fieldUser = new JTextField("SYSDBA", 16);
    private final JPasswordField fieldPass = new JPasswordField(16);
    private final ConnectionManagementService connService;
    private final SessionState session;

    /**
     * @param connService 连接服务
     * @param session     共用连接 ID
     */
    public ConnectionPanel(ConnectionManagementService connService, SessionState session) {
        this.connService = connService;
        this.session = session;
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        int row = 0;
        addPair(gbc, row++, new JLabel("连接 ID"), fieldId);
        addPair(gbc, row++, new JLabel("主机"), fieldHost);
        addPair(gbc, row++, new JLabel("端口"), fieldPort);
        addPair(gbc, row++, new JLabel("使用者"), fieldUser);
        addPair(gbc, row++, new JLabel("密码"), fieldPass);
        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton reg = new JButton("连接数据库");
        reg.addActionListener(e -> onRegister());
        JButton test = new JButton("#测试连接#");
        test.addActionListener(e -> onTest());
        btnRow.add(reg);
        btnRow.add(test);
        add(btnRow, gbc);
    }

    private void addPair(GridBagConstraints gbc, int row, JLabel label, JTextField field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        add(label, gbc);
        gbc.gridx = 1;
        add(field, gbc);
    }

    private void onRegister() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                DbConnectionProfile p = readProfile();
                connService.registerConnection(p);
                session.setConnectionId(p.getId());
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
