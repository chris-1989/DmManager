package com.dmadmin.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * 加载 classpath 与可选外部 {@code dm-admin-local.properties}。
 */
public final class AppProperties {

    private static final Logger LOG = LoggerFactory.getLogger(AppProperties.class);
    private static final String DEFAULT_RESOURCE = "/application.properties";
    private static final String LOCAL_FILE = "dm-admin-local.properties";

    private final Properties props = new Properties();

    /**
     * 从预设资源与工作目录下的本地档加载设定。
     *
     * @throws IOException 读档失败
     */
    public AppProperties() throws IOException {
        try (InputStream in = AppProperties.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (in != null) {
                props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        }
        Path local = Paths.get(LOCAL_FILE);
        if (Files.isRegularFile(local)) {
            try (InputStream in = Files.newInputStream(local)) {
                props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
                LOG.info("已加载外部设定档: {}", local.toAbsolutePath());
            }
        }
    }

    /**
     * @param key          键
     * @param defaultValue 预设值
     * @return 设定值
     */
    public String getString(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    /**
     * @param key          键
     * @param defaultValue 预设值
     * @return 整数设定
     */
    public int getInt(String key, int defaultValue) {
        String v = props.getProperty(key);
        if (v == null || v.trim().isEmpty()) {
            return defaultValue;
        }
        return Integer.parseInt(v.trim());
    }

    /**
     * @param key          键
     * @param defaultValue 预设值
     * @return 布林设定
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        String v = props.getProperty(key);
        if (v == null || v.trim().isEmpty()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(v.trim());
    }
}
