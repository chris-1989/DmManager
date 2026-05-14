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
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.nio.charset.Charset;

/**
 * DMP 导入：模式、路径、选项与日志输出（背景线程）。
 */
public class DmpImportPanel extends JPanel {

    private final AppProperties props;
    private final JTextField fieldHost = new JTextField("127.0.0.1", 12);
    private final JTextField fieldPort = new JTextField("5236", 6);
    private final JTextField fieldUser = new JTextField("SYSDBA", 12);
    private final JPasswordField fieldPass = new JPasswordField(12);
    private final JComboBox<ImportMode> comboMode = new JComboBox<>(ImportMode.values());
    private final JTextField fieldSchema = new JTextField(16);
    private final JTextField fieldTable = new JTextField(16);
    private final JTextField fieldDmp = new JTextField(32);
    private final JTextField fieldLog = new JTextField(32);
    private final JTextField fieldRemap = new JTextField(24);

    private final JTextField dmpToolPath = new JTextField(32);

    // 新增：文件选择按钮
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
     * @param props 应用设定（JNI 字元集、原生库目录等）
     */
    public DmpImportPanel(AppProperties props) {
        this.props = props;

        comboMode.setSelectedItem(ImportMode.FULL_DATABASE); // 默认选中整库导入
        comboMode.setEnabled(false); // 关闭下拉框选择功能

        fieldSchema.setVisible(false);
        fieldTable.setVisible(false);
        fieldRemap.setVisible(false);

        // 新增：绑定文件选择按钮事件
        bindFileChooserEvents();

        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(buildForm(), BorderLayout.NORTH);
        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.CENTER);
        progress.setStringPainted(true);
        JButton run = new JButton("开始导入");
        run.addActionListener(e -> runImport());
        JPanel south = new JPanel(new BorderLayout(4, 4));
        south.add(progress, BorderLayout.CENTER);
        south.add(run, BorderLayout.EAST);
        add(south, BorderLayout.SOUTH);
    }

    /**
     * 新增：绑定文件选择按钮的事件监听
     */
    private void bindFileChooserEvents() {
        // 选择dimp.exe文件
        btnSelectDmpTool.addActionListener(e -> {
            FileFilter exeFilter = new FileNameExtensionFilter("可执行文件 (*.exe)", "exe");
            String path = selectFile("选择dimp.exe执行文件", "选择", exeFilter);
            if (path != null) {
                dmpToolPath.setText(path);
            }
        });

        // 选择DMP数据文件
        btnSelectDmp.addActionListener(e -> {
            FileFilter dmpFilter = new FileNameExtensionFilter("DMP数据文件 (*.dmp)", "dmp");
            String path = selectFile("选择DMP数据文件", "选择", dmpFilter);
            if (path != null) {
                fieldDmp.setText(path);
            }
        });

        // 选择日志文件（支持新建/选择已有）
        btnSelectLog.addActionListener(e -> {
            FileFilter logFilter = new FileNameExtensionFilter("日志文件 (*.log)", "log");
            String path = selectFile("选择/新建日志文件", "选择", logFilter);
            if (path != null) {
                fieldLog.setText(path);
            }
        });
    }

    /**
     * 新增：通用文件选择对话框
     * @param title 对话框标题
     * @param approveText 确认按钮文本
     * @param filter 文件类型过滤器
     * @return 选中的文件绝对路径，取消则返回null
     */
    private String selectFile(String title, String approveText, FileFilter filter) {
        JFileChooser fileChooser = new JFileChooser();
        // 设置对话框标题
        fileChooser.setDialogTitle(title);
        // 设置确认按钮文本
        fileChooser.setApproveButtonText(approveText);
        // 设置文件类型过滤
        if (filter != null) {
            fileChooser.setFileFilter(filter);
        }
        // 仅允许选择文件（不选文件夹）
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        // 允许用户输入新文件名（比如日志文件可以新建）
        fileChooser.setAcceptAllFileFilterUsed(true);

        // 显示文件选择对话框
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            // 返回选中文件的绝对路径
            return selectedFile.getAbsolutePath();
        }
        return null;
    }

    private JPanel buildForm() {
        JPanel p = new JPanel(new GridLayout(0, 2, 6, 6));
        p.add(new JLabel("主机"));
        p.add(fieldHost);
        p.add(new JLabel("端口"));
        p.add(fieldPort);
        p.add(new JLabel("使用者"));
        p.add(fieldUser);
        p.add(new JLabel("密码"));
        p.add(fieldPass);
        p.add(new JLabel("导入模式"));
        p.add(comboMode);

        // 改造：dimp.exe路径 - 文本框+选择按钮
        JPanel panelDmpTool = new JPanel(new BorderLayout(4, 4));
        panelDmpTool.add(dmpToolPath, BorderLayout.CENTER);
        panelDmpTool.add(btnSelectDmpTool, BorderLayout.EAST);
        p.add(new JLabel("运行dimp.exe文件路径"));
        p.add(panelDmpTool);

        // 改造：DMP文件路径 - 文本框+选择按钮
        JPanel panelDmp = new JPanel(new BorderLayout(4, 4));
        panelDmp.add(fieldDmp, BorderLayout.CENTER);
        panelDmp.add(btnSelectDmp, BorderLayout.EAST);
        p.add(new JLabel("DMP 档路径"));
        p.add(panelDmp);

        // 改造：日志文件路径 - 文本框+选择按钮
        JPanel panelLog = new JPanel(new BorderLayout(4, 4));
        panelLog.add(fieldLog, BorderLayout.CENTER);
        panelLog.add(btnSelectLog, BorderLayout.EAST);
        p.add(new JLabel("日志档路径"));
        p.add(panelLog);

//        JPanel flags = new JPanel(new FlowLayout(FlowLayout.LEFT));
//        flags.add(chkIgnore);
//        flags.add(chkData);
//        flags.add(chkIdx);
//        flags.add(chkCons);
//        flags.add(chkRollback);
//        p.add(new JLabel("选项"));
//        p.add(flags);
        return p;
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