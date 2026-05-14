package com.dmadmin;

import com.dmadmin.ui.DmAdminSwingLauncher;
import com.dmadmin.dmp.DmpImportContext;
import com.dmadmin.dmp.ImportMode;
import com.dmadmin.dmp.ImportProgressListener;
import com.dmadmin.dmp.ImportProgressNotifier;
import com.dmadmin.exception.DmAdminException;
import com.dmadmin.model.DbConnectionProfile;
import com.dmadmin.model.DmpImportOptions;
import com.dmadmin.pool.ConnectionPoolManager;
import com.dmadmin.service.ConnectionManagementService;
import com.dmadmin.service.DmpImportService;
import com.dmadmin.util.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * 命令列入口：演示注册连接、测试连接、（可选）建立使用者与 DMP 导入。
 */
public final class DmAdminApplication {

    private static final Logger LOG = LoggerFactory.getLogger(DmAdminApplication.class);

    private DmAdminApplication() {
    }

    /**
     * 程序进入点。
     *
     * @param args 无参数或 gui：启动 Swing；help | test-connection | demo-import 为 CLI
     */
    public static void main(String[] args) throws IOException {
        if (args.length == 0 || "gui".equalsIgnoreCase(args[0])) {
            DmAdminSwingLauncher.launch();
            return;
        }
        if ("help".equalsIgnoreCase(args[0]) || "-h".equals(args[0]) || "--help".equals(args[0])) {
            printHelp();
            return;
        }
        AppProperties props = new AppProperties();
        ConnectionPoolManager pools = ConnectionPoolManager.getInstance();
        Runtime.getRuntime().addShutdownHook(new Thread(pools::shutdownAll));
        String cmd = args[0].toLowerCase();
        try {
            if ("test-connection".equals(cmd)) {
                runTestConnection(args, props, pools);
            } else {
                printHelp();
            }
        } catch (DmAdminException e) {
            LOG.error("执行失败: {}", e.getMessage(), e);
            System.exit(2);
        } catch (Exception e) {
            LOG.error("未预期错误", e);
            System.exit(3);
        }
    }

    private static void runTestConnection(String[] args, AppProperties props, ConnectionPoolManager pools)
            throws Exception {
        if (args.length < 6) {
            System.err.println("用法: test-connection <id> <host> <port> <user> <password>");
            System.exit(1);
        }
        String id = args[1];
        String host = args[2];
        int port = Integer.parseInt(args[3]);
        String user = args[4];
        String pass = args[5];
        DbConnectionProfile p = new DbConnectionProfile(id, host, port, user, pass);
        ConnectionManagementService svc = new ConnectionManagementService(pools, props);
        svc.registerConnection(p);
        boolean ok = svc.testConnection(id);
        LOG.info("连接测试结果: {}", ok);
    }


    private static void printHelp() {
        System.out.println("达梦管理工具");
        System.out.println("  GUI（无参数或 gui）：");
        System.out.println("  java -cp \"target/classes;lib/DmJdbcDriver18.jar\" com.dmadmin.DmAdminApplication");
        System.out.println("达梦管理工具 CLI");
        System.out.println("  java -cp \"target/classes;lib/DmJdbcDriver18.jar\" com.dmadmin.DmAdminApplication help");
        System.out.println("  ... test-connection <id> <host> <port> <user> <password>");
        System.out.println("  ... demo-import <host> <port> <user> <pass> TABLE|SCHEMA|FULL "
                + "<schema|null> <table|null> <dmpPath> <logPath> [remap]");
    }
}
