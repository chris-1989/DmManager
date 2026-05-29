package com.dmadmin.service;

import com.dmadmin.dmp.ImportProgressNotifier;
import com.dmadmin.dmp.ProgressParser;
import com.dmadmin.exception.DmAdminException;
import com.dmadmin.model.DbConnectionProfile;
import com.dmadmin.util.AppProperties;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class DmpExportService {

    private final String dexpToolPath;
    private final Charset nativeCharset;

    public DmpExportService(AppProperties props) {
        this.dexpToolPath = props.getString("dm.dexp.tool.path", "");
        this.nativeCharset = Charset.forName(props.getString("dm.dimp.native.charset", "GB18030"));
    }

    public void exportDmp(DbConnectionProfile profile, String owner,
                          String dmpOutput, String logOutput,
                          ImportProgressNotifier notifier) throws DmAdminException {
        String toolPath = new File(dexpToolPath).getAbsolutePath();
        String userId = profile.getUsername() + "/" + profile.getPassword()
                + "@" + profile.getHost() + ":" + profile.getPort();
        String filePath = new File(dmpOutput).getAbsolutePath();
        String logPath = new File(logOutput).getAbsolutePath();

        String[] command = {
                toolPath,
                "USERID=" + userId,
                "FILE=" + filePath,
                "OWNER=" + owner,
                "LOG=" + logPath
        };

        try {
            notifier.notifyLog("===== 开始达梦DMP导出 =====");
            notifier.notifyLog("执行命令：" + String.join(" ", command));
            notifier.notifyProgress(0);

            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), nativeCharset))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    notifier.notifyLog("[导出日志] " + line);
                    Integer progress = ProgressParser.parsePercent(line);
                    if (progress != null) {
                        notifier.notifyProgress(progress);
                    }
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0 || exitCode == 1 || exitCode == 2) {
                notifier.notifyProgress(100);
                notifier.notifyLog("导出完成！");
                if (exitCode >= 1) {
                    notifier.notifyLog("导出含警告，不影响使用，日志：" + logPath);
                }
            } else {
                String err = "导出失败，退出码：" + exitCode;
                notifier.notifyLog(err);
                notifier.notifyProgress(0);
                throw new DmAdminException(err);
            }
        } catch (DmAdminException e) {
            throw e;
        } catch (Exception e) {
            String err = "导出异常：" + e.getMessage();
            notifier.notifyLog(err);
            notifier.notifyProgress(0);
            throw new DmAdminException(err, e);
        }
    }
}
