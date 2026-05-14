package com.dmadmin.ui;

import com.dmadmin.pool.ConnectionPoolManager;
import com.dmadmin.service.ConnectionManagementService;
import com.dmadmin.util.AppProperties;

import javax.swing.JTabbedPane;
import javax.swing.JFrame;
import java.awt.BorderLayout;

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
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("连接", new ConnectionPanel(connSvc, session));
        tabs.addTab("使用者", new UserManagementPanel(poolManager, session));
        tabs.addTab("DMP 导入", new DmpImportPanel(props));
        add(tabs, BorderLayout.CENTER);
    }
}
