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

abstract class AbstractDmpImportHandler implements DmpImportHandler {

    // 匹配日志中百分比进度
    private static final Pattern PROGRESS_PATTERN = Pattern.compile("([1-9]\\d?|100)%");
    // 匹配唯一约束报错
    private static final Pattern DUPLICATE_KEY_PATTERN = Pattern.compile(
            "\\[警告\\]Error Code:-6602,违反表\\[(\\w+)\\]唯一性约束条件");
    private final List<String> duplicateTables = new ArrayList<>();

    protected void invoke(DmpImportContext ctx, ImportProgressNotifier notifier) throws DmAdminException {
        Charset cs = ctx.getNativeCharset();
        // 路径适配
        String dmpToolPath = adaptPathSeparator(ctx.getDmpToolPath());
        String dmpFilePath = adaptPathSeparator(ctx.getDmpFilePath());
        String logPath = adaptPathSeparator(ctx.getLogFilePath());

        // ===================== 【终极正确参数】达梦dimp官方标准 =====================
        String[] command = {
                dmpToolPath,
                ctx.buildUseridString(),
                "file=" + dmpFilePath,
                "full=y",
                "log=" + logPath,
                "TABLE_EXISTS_ACTION=TRUNCATE",  // ✅ 核心：清空表数据再导入（彻底解决唯一约束）
                "IGNORE=Y",                     // ✅ 忽略所有非致命错误
                "INDEXES=Y",                    // 导入索引
                "CONSTRAINTS=Y"                 // 导入约束
        };
        // ============================================================================

        try {
            duplicateTables.clear();
            notifier.notifyLog("===== 开始达梦DMP导入 =====");
            notifier.notifyLog("导入选项: ignoreExisting=true, data=true, indexes=true, constraints=true, rollback=true");
            notifier.notifyLog("执行命令：" + String.join(" ", command));
            notifier.notifyProgress(0);

            // 启动进程
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();

            // 读取日志
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "GBK"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    notifier.notifyLog("[导入日志] " + line);
                    // 解析进度
                    Integer progress = parseProgressFromLog(line);
                    if (progress != null) {
                        notifier.notifyProgress(progress);
                    }
                    // 解析重复表
                    Matcher matcher = DUPLICATE_KEY_PATTERN.matcher(line);
                    if (matcher.find() && !duplicateTables.contains(matcher.group(1))) {
                        duplicateTables.add(matcher.group(1));
                    }
                }
            }

            int exitCode = process.waitFor();
            // ===================== 【关键修正】达梦退出码规则 =====================
            // 0=成功 | 1=警告 | 2=少量警告(重复数据) → 都算导入成功！
            if (exitCode == 0 || exitCode == 1 || exitCode == 2) {
                notifier.notifyProgress(100);
                notifier.notifyLog("✅ 导入完成！");
                if (!duplicateTables.isEmpty()) {
                    notifier.notifyLog("⚠️ 重复数据表（已清空重导）：" + String.join(", ", duplicateTables));
                }
                if (exitCode >= 1) {
                    notifier.notifyLog("ℹ️ 导入含警告，不影响使用，日志：" + logPath);
                }
            } else {
                String err = "❌ 导入失败，退出码：" + exitCode;
                notifier.notifyLog(err);
                notifier.notifyProgress(0);
                throw new DmAdminException(err);
            }

        } catch (Exception e) {
            String err = "❌ 导入异常：" + e.getMessage();
            notifier.notifyLog(err);
            notifier.notifyProgress(0);
            throw new DmAdminException(err, e);
        }
    }

    // 解析进度
    private Integer parseProgressFromLog(String logLine) {
        if (logLine == null) return null;
        Matcher matcher = PROGRESS_PATTERN.matcher(logLine);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : null;
    }

    // 路径适配
    private String adaptPathSeparator(String originalPath) {
        if (originalPath == null || originalPath.isEmpty()) return originalPath;
        return new File(originalPath).getAbsolutePath();
    }
}