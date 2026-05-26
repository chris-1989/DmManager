package com.dmadmin.ui;

import com.dmadmin.dmp.DmpImportContext;
import com.dmadmin.dmp.ImportMode;
import com.dmadmin.dmp.ImportProgressListener;
import com.dmadmin.dmp.ImportProgressNotifier;
import com.dmadmin.model.DbConnectionProfile;
import com.dmadmin.model.DmpImportOptions;
import com.dmadmin.service.DmpImportService;
import com.dmadmin.util.AppProperties;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
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
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.FilenameFilter;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * DMP 批量导入：扫描文件夹内所有 .dmp 文件，逐一导入，日志自动命名。
 */
public class DmpImportPanel extends JPanel {

    private final AppProperties props;
    private final SessionState session;
    private final JTextField fieldHost = new JTextField("127.0.0.1", 16);
    private final JTextField fieldPort = new JTextField("5236", 8);
    private final JTextField fieldUser = new JTextField("SYSDBA", 16);
    private final JPasswordField fieldPass = new JPasswordField(16);
    private final JComboBox<ImportMode> comboMode = new JComboBox<>(ImportMode.values());
    private final JTextField fieldSchema = new JTextField(16);
    private final JTextField fieldTable = new JTextField(16);
    private final JTextField fieldDmpDir = new JTextField(32);
    private final JTextField fieldRemap = new JTextField(24);

    private final JTextField dmpToolPath = new JTextField(32);

    private final JButton btnSelectDmpTool = new JButton("选择");
    private final JButton btnSelectDmpDir = new JButton("选择");

    private final JCheckBox chkIgnore = new JCheckBox("忽略已存在表", true);
    private final JCheckBox chkData = new JCheckBox("导入资料", true);
    private final JCheckBox chkIdx = new JCheckBox("导入索引", true);
    private final JCheckBox chkCons = new JCheckBox("导入约束", true);
    private final JCheckBox chkRollback = new JCheckBox("失败时标记回滚策略", true);
    private final JTextArea logArea = new JTextArea(14, 60);
    private final JProgressBar progress = new JProgressBar(0, 100);

    /**
     * @param props   应用设定
     * @param session 跨分页共享的连接信息
     */
    public DmpImportPanel(AppProperties props, SessionState session) {
        this.props = props;
        this.session = session;

        String defaultDimpPath = props.getString("dm.dimp.tool.path", "");
        if (!defaultDimpPath.isEmpty()) {
            defaultDimpPath = new File(defaultDimpPath).getPath();
        } else {
            String nativeDir = props.getString("dm.dimp.native.library.dir", "");
            if (!nativeDir.isEmpty()) {
                defaultDimpPath = new File(nativeDir, "dimp.exe").getPath();
            }
        }
        dmpToolPath.setText(defaultDimpPath);

        comboMode.setSelectedItem(ImportMode.FULL_DATABASE);
        comboMode.setEnabled(false);

        fieldSchema.setVisible(false);
        fieldTable.setVisible(false);
        fieldRemap.setVisible(false);

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
        com.dmadmin.model.DbConnectionProfile profile = session.getConnectionProfile();
        if (profile == null) return;
        fieldHost.setText(profile.getHost());
        fieldPort.setText(String.valueOf(profile.getPort()));
        fieldUser.setText(profile.getUsername());
        fieldPass.setText(profile.getPassword());
    }

    private void bindFileChooserEvents() {
        btnSelectDmpTool.addActionListener(e -> {
            FileFilter exeFilter = new FileNameExtensionFilter("可执行文件 (*.exe)", "exe");
            String path = selectFile("选择dimp.exe执行文件", "选择", exeFilter);
            if (path != null) {
                dmpToolPath.setText(path);
            }
        });

        btnSelectDmpDir.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("选择 DMP 文件夹");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setAcceptAllFileFilterUsed(false);
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File dir = chooser.getSelectedFile();
                fieldDmpDir.setText(dir.getAbsolutePath());
            }
        });
    }

    private String selectFile(String title, String approveText, FileFilter filter) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(title);
        fileChooser.setApproveButtonText(approveText);
        if (filter != null) {
            fileChooser.setFileFilter(filter);
        }
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(true);
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            return selectedFile.getAbsolutePath();
        }
        return null;
    }

    private JPanel buildTopPanel() {
        JPanel top = new JPanel(new BorderLayout(8, 8));

        JPanel connPanel = buildConnectionSection();
        JPanel importPanel = buildImportSection();

        top.add(connPanel, BorderLayout.WEST);
        top.add(importPanel, BorderLayout.CENTER);
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
        addFormRow(panel, gbc, 3, "密码", fieldPass);

        return panel;
    }

    private JPanel buildImportSection() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("批量导入配置"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addFormRow(panel, gbc, 0, "导入模式", comboMode);

        JPanel toolPathPanel = new JPanel(new BorderLayout(4, 0));
        toolPathPanel.add(dmpToolPath, BorderLayout.CENTER);
        toolPathPanel.add(btnSelectDmpTool, BorderLayout.EAST);
        addFormRow(panel, gbc, 1, "dimp.exe 路径", toolPathPanel);

        JPanel dmpDirPanel = new JPanel(new BorderLayout(4, 0));
        dmpDirPanel.add(fieldDmpDir, BorderLayout.CENTER);
        dmpDirPanel.add(btnSelectDmpDir, BorderLayout.EAST);
        addFormRow(panel, gbc, 2, "DMP 文件夹路径", dmpDirPanel);

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
        panel.setBorder(BorderFactory.createTitledBorder("导入日志"));

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

        JButton runBtn = new JButton("开始批量导入");
        runBtn.addActionListener(e -> runImport());
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

    /**
     * @return 日志文件保存目录
     */
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

    /**
     * 检查目标用户是否已在数据库中有数据。
     * 逻辑：用户不存在 → 无数据；用户存在但 DBA_TABLES 无表 → 无数据。
     *
     * @param host     主机
     * @param port     端口
     * @param username 使用者
     * @param password 密码
     * @return 若用户已有表则返回 true
     */
    private boolean checkUserHasData(String host, int port, String loginUsername, String username, String password) {
        String jdbcUrl = "jdbc:dm://" + host + ":" + port;
        String safeUser = username.toUpperCase().replace("'", "''");
        try {
            Class.forName("dm.jdbc.driver.DmDriver");
        } catch (ClassNotFoundException e) {
            appendLog("[警告] 无法加载达梦驱动，跳过验证: " + e.getMessage());
            return false;
        }
        try (Connection conn = DriverManager.getConnection(jdbcUrl, loginUsername, password);
             Statement stmt = conn.createStatement()) {

            // 先查用户是否存在
            String checkUserSql = "SELECT COUNT(*) FROM DBA_USERS WHERE USERNAME = '" + safeUser + "'";
            int userCount = 0;
            try (ResultSet rs = stmt.executeQuery(checkUserSql)) {
                if (rs.next()) {
                    userCount = rs.getInt(1);
                }
            } catch (Exception e) {
                appendLog("[提示] DBA_USERS 查询失败，尝试 DBA_TABLES: " + e.getMessage());
            }

            if (userCount == 0) {
                appendLog("[验证] 用户 " + username + " 在数据库中不存在，视为无数据");
                return false;
            }

            // 用户存在，检查其是否有表
            String checkTablesSql = "SELECT COUNT(*) FROM DBA_TABLES WHERE OWNER = '" + safeUser + "'";
            try (ResultSet rs = stmt.executeQuery(checkTablesSql)) {
                if (rs.next()) {
                    int tableCount = rs.getInt(1);
                    return tableCount > 0;
                }
            }
        } catch (Exception e) {
            appendLog("[警告] 数据验证查询失败: " + e.getMessage() + "，跳过验证继续导入");
        }
        return false;
    }

    private void runImport() {
        String dmpDirPath = fieldDmpDir.getText().trim();
        if (dmpDirPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请选择 DMP 文件夹路径", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        File dmpDir = new File(dmpDirPath);
        if (!dmpDir.isDirectory()) {
            JOptionPane.showMessageDialog(this, "DMP 文件夹路径无效", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        File[] dmpFiles = dmpDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".dmp");
            }
        });
        if (dmpFiles == null || dmpFiles.length == 0) {
            JOptionPane.showMessageDialog(this, "未找到任何 .dmp 文件", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        logArea.setText("");
        progress.setValue(0);

        int total = dmpFiles.length;
        String timePrefix = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File logDir = resolveLogDir();
        appendLog("日志目录: " + logDir.getAbsolutePath());
        appendLog("找到 " + total + " 个 DMP 文件，开始批量导入...\n");

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

        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                int port = Integer.parseInt(fieldPort.getText().trim());
                DbConnectionProfile profile = new DbConnectionProfile(
                        "gui-batch-import",
                        fieldHost.getText().trim(),
                        port,
                        fieldUser.getText().trim(),
                        new String(fieldPass.getPassword()));
                DmpImportOptions opt = new DmpImportOptions();
                opt.setIgnoreExistingTables(chkIgnore.isSelected());
                opt.setImportData(chkData.isSelected());
                opt.setImportIndexes(chkIdx.isSelected());
                opt.setImportConstraints(chkCons.isSelected());
                opt.setRollbackOnFailure(chkRollback.isSelected());
                ImportMode mode = (ImportMode) comboMode.getSelectedItem();
                Charset cs = Charset.forName(props.getString("dm.dimp.native.charset", "GB18030"));
                DmpImportService svc = new DmpImportService(props);

                String targetHost = fieldHost.getText().trim();

                for (int i = 0; i < dmpFiles.length; i++) {
                    File dmpFile = dmpFiles[i];
                    String dmpName = dmpFile.getName();
                    // 从 DMP 文件名提取目标用户名（去掉 .dmp 后缀）
                    String dmpUser = dmpName.replaceAll("(?i)\\.dmp$", "");
                    String logFileName = dmpUser + "_" + timePrefix + ".log";
                    String logFilePath = new File(logDir, logFileName).getAbsolutePath();

                    publish("[" + (i + 1) + "/" + total + "] 检查: " + dmpName + " (目标用户: " + dmpUser + ")");

                    if (checkUserHasData(targetHost, port, profile.getUsername(), dmpUser, new String(fieldPass.getPassword()))) {
                        publish("[警告] 用户 " + dmpUser + " 在数据库中已有数据，跳过导入: " + dmpName);
                        publish("  日志已保存: " + logFilePath);
                        writeSkipLog(logFilePath, dmpUser, dmpName);
                        int pct = (int) (((i + 1) * 100L) / total);
                        publish("__PROGRESS__" + pct);
                        continue;
                    }

                    publish("[" + (i + 1) + "/" + total + "] 开始导入: " + dmpName);
                    publish("  日志文件: " + logFilePath);

                    try {
                        DmpImportContext ctx = new DmpImportContext(
                                mode,
                                profile,
                                fieldSchema.getText().trim(),
                                fieldTable.getText().trim(),
                                dmpFile.getAbsolutePath(),
                                logFilePath,
                                fieldRemap.getText().trim(),
                                opt,
                                cs,
                                dmpToolPath.getText().trim());
                        svc.importDmp(ctx, notifier);
                        publish("[" + (i + 1) + "/" + total + "] 导入完成: " + dmpName + "\n");
                    } catch (Exception ex) {
                        Throwable t = unwrap(ex);
                        publish("[" + (i + 1) + "/" + total + "] 导入失败: " + dmpName + " - " + t.getMessage() + "\n");
                    }

                    int pct = (int) (((i + 1) * 100L) / total);
                    publish("__PROGRESS__" + pct);
                }
                publish("========== 批量导入结束，共处理 " + total + " 个文件 ==========");
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
                    appendLog("--- 批量导入流程结束 ---");
                    JOptionPane.showMessageDialog(DmpImportPanel.this, "批量导入完成。", "DMP",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    Throwable t = unwrap(ex);
                    appendLog("错误: " + t.getMessage());
                    JOptionPane.showMessageDialog(DmpImportPanel.this,
                            t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName(),
                            "DMP 批量导入失败", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void writeSkipLog(String logFilePath, String username, String dmpName) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("========== DMP 导入跳过日志 ==========\n");
            sb.append("时间: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\n");
            sb.append("DMP 文件: ").append(dmpName).append("\n");
            sb.append("原因: 用户 ").append(username).append(" 在数据库中已有数据，为避免覆盖而跳过\n");
            sb.append("======================================\n");
            java.nio.file.Files.write(java.nio.file.Paths.get(logFilePath), sb.toString().getBytes("UTF-8"));
        } catch (Exception e) {
            appendLog("[警告] 写入跳过日志失败: " + e.getMessage());
        }
    }

    private static Throwable unwrap(Exception ex) {
        Throwable t = ex;
        if (ex instanceof java.util.concurrent.ExecutionException && ex.getCause() != null) {
            t = ex.getCause();
        }
        return t;
    }
}
