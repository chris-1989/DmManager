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
import java.nio.charset.Charset;

/**
 * DMP 导入：模式、路径、选项与日志输出（背景线程）。
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
    private final JTextField fieldDmp = new JTextField(32);
    private final JTextField fieldLog = new JTextField(32);
    private final JTextField fieldRemap = new JTextField(24);

    private final JTextField dmpToolPath = new JTextField(32);

    private final JButton btnSelectDmpTool = new JButton("选择");
    private final JButton btnSelectDmp = new JButton("选择");
    private final JButton btnSelectLog = new JButton("选择");

    private final JCheckBox chkIgnore = new JCheckBox("忽略已存在表", true);
    private final JCheckBox chkData = new JCheckBox("导入资料", true);
    private final JCheckBox chkIdx = new JCheckBox("导入索引", true);
    private final JCheckBox chkCons = new JCheckBox("导入约束", true);
    private final JCheckBox chkRollback = new JCheckBox("失败时标记回滚策略", true);
    private final JTextArea logArea = new JTextArea(14, 60);
    private final JProgressBar progress = new JProgressBar(0, 100);

    /**
     * @param props   应用设定（JNI 字元集、原生库目录等）
     * @param session 跨分页共享的连接信息
     */
    public DmpImportPanel(AppProperties props, SessionState session) {
        this.props = props;
        this.session = session;

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

    /**
     * 从 SessionState 同步连接信息到表单字段。
     */
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

        btnSelectDmp.addActionListener(e -> {
            FileFilter dmpFilter = new FileNameExtensionFilter("DMP数据文件 (*.dmp)", "dmp");
            String path = selectFile("选择DMP数据文件", "选择", dmpFilter);
            if (path != null) {
                fieldDmp.setText(path);
            }
        });

        btnSelectLog.addActionListener(e -> {
            FileFilter logFilter = new FileNameExtensionFilter("日志文件 (*.log)", "log");
            String path = selectFile("选择/新建日志文件", "选择", logFilter);
            if (path != null) {
                fieldLog.setText(path);
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
        panel.setBorder(BorderFactory.createTitledBorder("导入配置"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addFormRow(panel, gbc, 0, "导入模式", comboMode);

        JPanel toolPathPanel = new JPanel(new BorderLayout(4, 0));
        toolPathPanel.add(dmpToolPath, BorderLayout.CENTER);
        toolPathPanel.add(btnSelectDmpTool, BorderLayout.EAST);
        addFormRow(panel, gbc, 1, "dimp.exe 路径", toolPathPanel);

        JPanel dmpPathPanel = new JPanel(new BorderLayout(4, 0));
        dmpPathPanel.add(fieldDmp, BorderLayout.CENTER);
        dmpPathPanel.add(btnSelectDmp, BorderLayout.EAST);
        addFormRow(panel, gbc, 2, "DMP 文件路径", dmpPathPanel);

        JPanel logPathPanel = new JPanel(new BorderLayout(4, 0));
        logPathPanel.add(fieldLog, BorderLayout.CENTER);
        logPathPanel.add(btnSelectLog, BorderLayout.EAST);
        addFormRow(panel, gbc, 3, "日志文件路径", logPathPanel);

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

        JButton runBtn = new JButton("开始导入");
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

    private void runImport() {
        logArea.setText("");
        progress.setValue(0);
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
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                int port = Integer.parseInt(fieldPort.getText().trim());
                DbConnectionProfile profile = new DbConnectionProfile(
                        "gui-import",
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
                DmpImportContext ctx = new DmpImportContext(
                        mode,
                        profile,
                        fieldSchema.getText().trim(),
                        fieldTable.getText().trim(),
                        fieldDmp.getText().trim(),
                        fieldLog.getText().trim(),
                        fieldRemap.getText().trim(),
                        opt,
                        cs,
                        dmpToolPath.getText().trim());
                DmpImportService svc = new DmpImportService(props);
                svc.importDmp(ctx, notifier);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    appendLog("--- 导入流程结束 ---");
                    JOptionPane.showMessageDialog(DmpImportPanel.this, "导入完成。", "DMP",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    Throwable t = unwrap(ex);
                    appendLog("错误: " + t.getMessage());
                    JOptionPane.showMessageDialog(DmpImportPanel.this,
                            t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName(),
                            "DMP 导入失败", JOptionPane.ERROR_MESSAGE);
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
