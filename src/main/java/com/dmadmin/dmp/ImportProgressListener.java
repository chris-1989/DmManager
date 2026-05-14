package com.dmadmin.dmp;

/**
 * 导入进度观察者：接收日志与百分比更新。
 */
public interface ImportProgressListener {

    /**
     * 导入过程日志或状态行。
     *
     * @param line 日志文字
     */
    void onLogLine(String line);

    /**
     * 进度百分比（若底层无回调则可能仅在开始/结束触发）。
     *
     * @param percent 0–100
     */
    void onProgressPercent(int percent);
}
