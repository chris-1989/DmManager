package com.dmadmin.ui;

import com.dmadmin.dmp.ImportProgressListener;
import com.dmadmin.dmp.ImportProgressNotifier;
import com.dmadmin.model.DbConnectionProfile;
import com.dmadmin.service.DmpExportService;
import com.dmadmin.util.AppProperties;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * DMP 导出：将指定用户下所有表导出为 DMP 文件。
 */
public class DmpExportPanel extends JPanel {

    private final AppProperties props;
    private final SessionState session;
    private final JTextField fieldHost = new JTextField("127.0.0.1", 16);
    private final JTextField fieldPort = new JTextField("5236", 8);
    private final JTextField fieldUser = new JTextField("SYSDBA", 16);
    private final JPasswordField fieldPass = new JPasswordField(16);
    private final JTextField fieldSelectedOwners = new JTextField(24);
    private final JButton btnSelectOwners = new JButton("选择");
    private final JTextField dexpToolPath = new JTextField(32);
    private final JTextField fieldOutputDir = new JTextField(32);

    private final JButton btnSelectDexpTool = new JButton("选择");
    private final JButton btnSelectOutputDir = new JButton("选择");

    private final JTextArea logArea = new JTextArea(14, 60);
    private final JProgressBar progress = new JProgressBar(0, 100);

    private List<String> allDbUsers = new ArrayList<>();
    private List<String> selectedOwners = new ArrayList<>();

    public DmpExportPanel(AppProperties props, SessionState session) {
        this.props = props;
        this.session = session;

        fieldSelectedOwners.setEditable(false);
        fieldSelectedOwners.setBackground(java.awt.Color.WHITE);

        String defaultDexpPath = props.getString("dm.dexp.tool.path", "");
        if (!defaultDexpPath.isEmpty()) {
            defaultDexpPath = new File(defaultDexpPath).getPath();
        }
        dexpToolPath.setText(defaultDexpPath);

        String desktop = System.getProperty("user.home") + File.separator + "Desktop";
        fieldOutputDir.setText(desktop);

        bindFileChooserEvents();

        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        add(buildTopPanel(), BorderLayout.NORTH);
        add(buildCenterPanel(), BorderLayout.CENTER);
        add(buildSouthPanel(), BorderLayout.SOUTH);

        syncFromSession();
    }

    public void syncFromSession() {
        if (session == null) return;
        DbConnectionProfile profile = session.getConnectionProfile();
        if (profile == null) return;
        fieldHost.setText(profile.getHost());
        fieldPort.setText(String.valueOf(profile.getPort()));
        fieldUser.setText(profile.getUsername());
        fieldPass.setText(profile.getPassword());
        refreshOwnerList();
    }

    private void refreshOwnerList() {
        allDbUsers.clear();
        DbConnectionProfile profile = session.getConnectionProfile();
        if (profile == null) return;
        String jdbcUrl = "jdbc:dm://" + profile.getHost() + ":" + profile.getPort();
        try {
            Class.forName("dm.jdbc.driver.DmDriver");
        } catch (ClassNotFoundException e) {
            return;
        }
        try (Connection conn = DriverManager.getConnection(jdbcUrl, profile.getUsername(), profile.getPassword());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT USERNAME FROM DBA_USERS ORDER BY USERNAME")) {
            while (rs.next()) {
                allDbUsers.add(rs.getString("USERNAME"));
            }
        } catch (Exception e) {
            appendLog("[提示] 加载用户列表失败: " + e.getMessage());
        }
    }

    private void openOwnerSelectionDialog() {
        Window parent = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parent, "选择目标用户", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout(8, 8));

        List<JCheckBox> checkBoxes = new ArrayList<>();
        JPanel checkboxPanel = new JPanel(new GridLayout(0, 1, 4, 2));
        checkboxPanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        for (String user : allDbUsers) {
            JCheckBox cb = new JCheckBox(user);
            cb.setSelected(selectedOwners.contains(user));
            checkBoxes.add(cb);
            checkboxPanel.add(cb);
        }

        JScrollPane scrollPane = new JScrollPane(checkboxPanel);
        scrollPane.setPreferredSize(new Dimension(280, 320));
        dialog.add(scrollPane, BorderLayout.CENTER);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton selectAllBtn = new JButton("全选");
        JButton deselectAllBtn = new JButton("取消全选");
        selectAllBtn.addActionListener(e -> {
            for (JCheckBox cb : checkBoxes) cb.setSelected(true);
        });
        deselectAllBtn.addActionListener(e -> {
            for (JCheckBox cb : checkBoxes) cb.setSelected(false);
        });
        topPanel.add(selectAllBtn);
        topPanel.add(deselectAllBtn);
        dialog.add(topPanel, BorderLayout.NORTH);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton okBtn = new JButton("确定");
        JButton cancelBtn = new JButton("取消");
        okBtn.addActionListener(e -> {
            selectedOwners.clear();
            for (JCheckBox cb : checkBoxes) {
                if (cb.isSelected()) {
                    selectedOwners.add(cb.getText());
                }
            }
            fieldSelectedOwners.setText(String.join(", ", selectedOwners));
            dialog.dispose();
        });
        cancelBtn.addActionListener(e -> dialog.dispose());
        bottomPanel.add(okBtn);
        bottomPanel.add(cancelBtn);
        dialog.add(bottomPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(true);
        dialog.setVisible(true);
    }

    private void bindFileChooserEvents() {
        btnSelectOwners.addActionListener(e -> openOwnerSelectionDialog());

        btnSelectDexpTool.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("选择 dexp.exe 执行文件");
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setAcceptAllFileFilterUsed(true);
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                dexpToolPath.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });

        btnSelectOutputDir.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("选择 DMP 输出目录");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setAcceptAllFileFilterUsed(false);
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                fieldOutputDir.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
    }

    private JPanel buildTopPanel() {
        JPanel top = new JPanel(new BorderLayout(8, 8));

        JPanel connPanel = buildConnectionSection();
        JPanel exportPanel = buildExportSection();

        top.add(connPanel, BorderLayout.WEST);
        top.add(exportPanel, BorderLayout.CENTER);
        return top;
    }

    private JPanel buildConnectionSection() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("数据库连接"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addFormRow(panel, gbc, 0, "主机", fieldHost);
        addFormRow(panel, gbc, 1, "端口", fieldPort);
        addFormRow(panel, gbc, 2, "使用者", fieldUser);
        addFormRow(panel, gbc, 3, "密码", createPasswordPanel(fieldPass));

        return panel;
    }

    private JPanel buildExportSection() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("导出配置"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JPanel ownerPanel = new JPanel(new BorderLayout(4, 0));
        ownerPanel.add(fieldSelectedOwners, BorderLayout.CENTER);
        ownerPanel.add(btnSelectOwners, BorderLayout.EAST);
        addFormRow(panel, gbc, 0, "目标用户", ownerPanel);

        JPanel toolPathPanel = new JPanel(new BorderLayout(4, 0));
        toolPathPanel.add(dexpToolPath, BorderLayout.CENTER);
        toolPathPanel.add(btnSelectDexpTool, BorderLayout.EAST);
        addFormRow(panel, gbc, 1, "dexp.exe 路径", toolPathPanel);

        JPanel outputDirPanel = new JPanel(new BorderLayout(4, 0));
        outputDirPanel.add(fieldOutputDir, BorderLayout.CENTER);
        outputDirPanel.add(btnSelectOutputDir, BorderLayout.EAST);
        addFormRow(panel, gbc, 2, "输出目录", outputDirPanel);

        return panel;
    }

    private JPanel createPasswordPanel(JPasswordField passField) {
        JPanel panel = new JPanel(new BorderLayout(4, 0));
        panel.add(passField, BorderLayout.CENTER);
        JButton toggleBtn = new JButton("显示");
        toggleBtn.setPreferredSize(new Dimension(60, passField.getPreferredSize().height));
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

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        JLabel jLabel = new JLabel(label);
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(jLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(field, gbc);
    }

    private JPanel buildCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createTitledBorder("导出日志"));

        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setPreferredSize(new Dimension(0, 280));
        panel.add(logScroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildSouthPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 4));

        progress.setStringPainted(true);
        progress.setPreferredSize(new Dimension(0, 22));
        panel.add(progress, BorderLayout.CENTER);

        JButton runBtn = new JButton("开始导出");
        runBtn.addActionListener(e -> runExport());
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnPanel.add(runBtn);
        panel.add(btnPanel, BorderLayout.EAST);

        return panel;
    }

    private void appendLog(String line) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(line + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private File resolveLogDir() {
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

    private void runExport() {
        if (selectedOwners.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请选择目标用户", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String outputDir = fieldOutputDir.getText().trim();
        if (outputDir.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请选择输出目录", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        File outDir = new File(outputDir);
        if (!outDir.isDirectory()) {
            JOptionPane.showMessageDialog(this, "输出目录路径无效", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        logArea.setText("");
        progress.setValue(0);

        String timePrefix = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File logDir = resolveLogDir();
        int total = selectedOwners.size();
        appendLog("日志目录: " + logDir.getAbsolutePath());
        appendLog("输出目录: " + outDir.getAbsolutePath());
        appendLog("已选择 " + total + " 个用户，开始批量导出...\n");

        ImportProgressNotifier notifier = new ImportProgressNotifier();
        notifier.addListener(new ImportProgressListener() {
            @Override
            public void onLogLine(String line) {
                appendLog(line);
            }

            @Override
            public void onProgressPercent(int percent) {
                SwingUtilities.invokeLater(() -> progress.setValue(Math.max(0, Math.min(100, percent))));
            }
        });

        List<String> owners = new ArrayList<>(selectedOwners);

        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                int port = Integer.parseInt(fieldPort.getText().trim());
                DbConnectionProfile profile = new DbConnectionProfile(
                        "gui-export",
                        fieldHost.getText().trim(),
                        port,
                        fieldUser.getText().trim(),
                        new String(fieldPass.getPassword()));
                DmpExportService svc = new DmpExportService(props);

                for (int i = 0; i < owners.size(); i++) {
                    String owner = owners.get(i);
                    String logFileName = owner + "_export_" + timePrefix + ".log";
                    String logFilePath = new File(logDir, logFileName).getAbsolutePath();
                    String dmpFileName = owner + "_" + timePrefix + ".dmp";
                    String dmpFilePath = new File(outDir, dmpFileName).getAbsolutePath();

                    publish("[" + (i + 1) + "/" + total + "] 开始导出: " + owner);
                    publish("  DMP 文件: " + dmpFileName);
                    publish("  日志文件: " + logFileName);

                    try {
                        svc.exportDmp(profile, owner, dmpFilePath, logFilePath, notifier);
                        publish("[" + (i + 1) + "/" + total + "] 导出完成: " + owner + "\n");
                    } catch (Exception ex) {
                        Throwable t = unwrap(ex);
                        publish("[" + (i + 1) + "/" + total + "] 导出失败: " + owner + " - " + t.getMessage() + "\n");
                    }

                    int pct = (int) (((i + 1) * 100L) / total);
                    publish("__PROGRESS__" + pct);
                }
                publish("========== 批量导出结束，共处理 " + total + " 个用户 ==========");
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String msg : chunks) {
                    if (msg.startsWith("__PROGRESS__")) {
                        int pct = Integer.parseInt(msg.substring("__PROGRESS__".length()));
                        progress.setValue(pct);
                    } else {
                        appendLog(msg);
                    }
                }
            }

            @Override
            protected void done() {
                try {
                    get();
                    appendLog("--- 批量导出流程结束 ---");
                    JOptionPane.showMessageDialog(DmpExportPanel.this,
                            "批量导出完成，共处理 " + total + " 个用户。", "DMP 导出",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    Throwable t = unwrap(ex);
                    appendLog("错误: " + t.getMessage());
                    JOptionPane.showMessageDialog(DmpExportPanel.this,
                            t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName(),
                            "DMP 导出失败", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private static Throwable unwrap(Exception ex) {
        Throwable t = ex;
        if (ex instanceof java.util.concurrent.ExecutionException && ex.getCause() != null) {
            t = ex.getCause();
        }
        return t;
    }
}
