package com.dmadmin.util;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.concurrent.ExecutionException;

/**
 * Swing 公共组件工厂方法。
 */
public final class SwingComponents {

    private SwingComponents() {
    }

    /**
     * 创建带显示/隐藏切换按钮的密码面板。
     */
    public static JPanel createPasswordPanel(JPasswordField passField) {
        JPanel panel = new JPanel(new BorderLayout(4, 0));
        panel.add(passField, BorderLayout.CENTER);
        JButton toggleBtn = new JButton("显示");
        toggleBtn.setPreferredSize(new Dimension(60, 28));
        toggleBtn.addActionListener(e -> {
            if (passField.getEchoChar() == 0) {
                passField.setEchoChar('*');
                toggleBtn.setText("显示");
            } else {
                passField.setEchoChar((char) 0);
                toggleBtn.setText("隐藏");
            }
        });
        panel.add(toggleBtn, BorderLayout.EAST);
        return panel;
    }

    /**
     * 向面板添加一行标签+字段。
     */
    public static void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        JLabel jLabel = new JLabel(label);
        jLabel.setPreferredSize(new Dimension(60, 28));
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        panel.add(jLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(field, gbc);
    }

    /**
     * 线程安全地向文本区域追加日志行。
     */
    public static void appendLog(JTextArea logArea, String line) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(line + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    /**
     * 创建日志展示面板（带标题和滚动条）。
     */
    public static JPanel buildLogPanel(JTextArea logArea, String title) {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createTitledBorder(title));

        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setPreferredSize(new Dimension(0, 280));
        panel.add(logScroll, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 创建底部操作面板（进度条 + 按钮）。
     */
    public static JPanel buildActionPanel(JProgressBar progress, JButton actionBtn) {
        JPanel panel = new JPanel(new BorderLayout(8, 4));

        progress.setStringPainted(true);
        progress.setPreferredSize(new Dimension(0, 22));
        panel.add(progress, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnPanel.add(actionBtn);
        panel.add(btnPanel, BorderLayout.EAST);

        return panel;
    }

    /**
     * 从配置解析日志目录，不存在则自动创建。
     */
    public static File resolveLogDir(AppProperties props) {
        String logDir = props.getString("dm.dimp.log.dir", "");
        if (logDir.isEmpty()) {
            logDir = new File("logs", "dmp").getAbsolutePath();
        }
        File dir = new File(logDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * 提取 SwingWorker 异常的根本原因。
     */
    public static Throwable unwrapCause(Exception ex) {
        Throwable t = ex;
        if (ex instanceof ExecutionException && ex.getCause() != null) {
            t = ex.getCause();
        }
        return t;
    }
}
