package com.dmadmin.ui;

import com.dmadmin.pool.ConnectionPoolManager;
import com.dmadmin.util.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.io.IOException;

/**
 * 启动 Swing 图形界面。
 */
public final class DmAdminSwingLauncher {

    private static final Logger LOG = LoggerFactory.getLogger(DmAdminSwingLauncher.class);

    private DmAdminSwingLauncher() {
    }

    /**
     * 在事件分派线程建立主窗口并显示。
     */
    public static void launch() {

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new FlatLightLaf());
            } catch (Exception e) {
                LOG.debug("使用预设外观: {}", e.toString());
            }
            AppProperties props;
            try {
                props = new AppProperties();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "加载设定失败：\n" + e.getMessage(),
                        "达梦管理工具", JOptionPane.ERROR_MESSAGE);
                return;
            }
            ConnectionPoolManager pools = ConnectionPoolManager.getInstance();
            Runtime.getRuntime().addShutdownHook(new Thread(pools::shutdownAll));
            MainFrame frame = new MainFrame(props, pools);
            frame.setVisible(true);
        });
    }
}
