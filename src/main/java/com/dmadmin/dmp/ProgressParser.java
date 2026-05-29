package com.dmadmin.dmp;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ProgressParser {

    private static final Pattern PROGRESS_PATTERN = Pattern.compile("([1-9]\\d?|100)%");

    private ProgressParser() {
    }

    /**
     * @param logLine 导入/导出日志行
     * @return 百分比 0-100，未找到返回 null
     */
    public static Integer parsePercent(String logLine) {
        if (logLine == null) return null;
        Matcher matcher = PROGRESS_PATTERN.matcher(logLine);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : null;
    }
}
