package com.dmadmin.dmp;

import com.dmadmin.exception.DmAdminException;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DmpImportExecutor {

    private static final Pattern DUPLICATE_KEY_PATTERN = Pattern.compile(
            "\\[警告\\]Error Code:-6602,违反表\\[(\\w+)\\]唯一性约束条件");
    private final List<String> duplicateTables = new ArrayList<>();

    public void execute(DmpImportContext ctx, ImportProgressNotifier notifier) throws DmAdminException {
        Charset cs = ctx.getNativeCharset();
        String dmpToolPath = adaptPathSeparator(ctx.getDmpToolPath());
        String dmpFilePath = adaptPathSeparator(ctx.getDmpFilePath());
        String logPath = adaptPathSeparator(ctx.getLogFilePath());

        String[] command = {
                dmpToolPath,
                ctx.buildUseridString(),
                "file=" + dmpFilePath,
                "full=y",
                "log=" + logPath,
                "TABLE_EXISTS_ACTION=TRUNCATE",
                "IGNORE=Y",
                "IGNORE_INIT_PARA=1",
                "INDEXES=Y",
                "CONSTRAINTS=Y"
        };

        try {
            duplicateTables.clear();
            notifier.notifyLog("===== 开始达梦DMP导入 =====");
            notifier.notifyLog("执行命令：" + String.join(" ", command));
            notifier.notifyProgress(0);

            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "GBK"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    notifier.notifyLog("[导入日志] " + line);
                    Integer progress = ProgressParser.parsePercent(line);
                    if (progress != null) {
                        notifier.notifyProgress(progress);
                    }
                    Matcher matcher = DUPLICATE_KEY_PATTERN.matcher(line);
                    if (matcher.find() && !duplicateTables.contains(matcher.group(1))) {
                        duplicateTables.add(matcher.group(1));
                    }
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0 || exitCode == 1 || exitCode == 2) {
                notifier.notifyProgress(100);
                notifier.notifyLog("导入完成！");
                if (!duplicateTables.isEmpty()) {
                    notifier.notifyLog("重复数据表（已清空重导）：" + String.join(", ", duplicateTables));
                }
                if (exitCode >= 1) {
                    notifier.notifyLog("导入含警告，不影响使用，日志：" + logPath);
                }
            } else {
                String err = "导入失败，退出码：" + exitCode;
                notifier.notifyLog(err);
                notifier.notifyProgress(0);
                throw new DmAdminException(err);
            }

        } catch (Exception e) {
            String err = "导入异常：" + e.getMessage();
            notifier.notifyLog(err);
            notifier.notifyProgress(0);
            throw new DmAdminException(err, e);
        }
    }

    private String adaptPathSeparator(String originalPath) {
        if (originalPath == null || originalPath.isEmpty()) return originalPath;
        return new File(originalPath).getAbsolutePath();
    }
}
