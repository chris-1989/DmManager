package com.dmadmin.ui;

import com.dmadmin.pool.ConnectionPoolManager;
import com.dmadmin.service.ConnectionManagementService;
import com.dmadmin.util.AppProperties;

import javax.swing.JTabbedPane;
import javax.swing.JFrame;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import java.awt.BorderLayout;
import java.awt.Dimension;

/**
 * 主窗口：连接、使用者、DMP 导入分页。
 */
public class MainFrame extends JFrame {

    private final SessionState session = new SessionState();

    /**
     * @param props        应用设定
     * @param poolManager  连接池单例
     */
    public MainFrame(AppProperties props, ConnectionPoolManager poolManager) {
        super();
        ConnectionManagementService connSvc = new ConnectionManagementService(poolManager, props);
        setLayout(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane(SwingConstants.TOP);
        tabs.setTabPlacement(JTabbedPane.TOP);
        tabs.addTab("连接", new ConnectionPanel(connSvc, session));
        tabs.addTab("使用者", new UserManagementPanel(poolManager, session));
        DmpImportPanel dmpPanel = new DmpImportPanel(props, session);
        tabs.addTab("DMP 导入", dmpPanel);
        tabs.addChangeListener((ChangeEvent e) -> {
            if (tabs.getSelectedComponent() == dmpPanel) {
                dmpPanel.syncFromSession();
            }
        });
        add(tabs, BorderLayout.CENTER);

        setTitle("达梦数据库管理工具");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 750);
        setMinimumSize(new Dimension(960, 640));
        setLocationRelativeTo(null);
    }
}
