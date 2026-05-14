package com.dmadmin.dmp;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 观察者集合：线程安全地广播导入事件。
 */
public class ImportProgressNotifier {

    private final List<ImportProgressListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * 注册监听器。
     *
     * @param listener 监听器
     */
    public void addListener(ImportProgressListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * @param line 日志行
     */
    public void notifyLog(String line) {
        for (ImportProgressListener l : listeners) {
            l.onLogLine(line);
        }
    }

    /**
     * @param percent 百分比
     */
    public void notifyProgress(int percent) {
        for (ImportProgressListener l : listeners) {
            l.onProgressPercent(percent);
        }
    }
}
