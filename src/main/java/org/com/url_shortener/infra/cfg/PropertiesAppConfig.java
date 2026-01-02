package org.com.url_shortener.infra.cfg;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;

public final class PropertiesAppConfig implements AppConfig {
    private static final String DEFAULT_BASE_URL = "short";
    private static final long DEFAULT_TTL_SECONDS = 86400; // 24h
    private static final int DEFAULT_DEFAULT_MAX_CLICKS = 10;
    private static final long DEFAULT_CLEANUP_INTERVAL_SECONDS = 30;
    private static final String DEFAULT_USER_UUID_FILE = ".url-shortener-cli/user.uuid";

    private final Properties props;

    public PropertiesAppConfig(String resourceName) {
        this.props = new Properties();
        try (InputStream in = PropertiesAppConfig.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (in != null) {
                this.props.load(in);
            } else {
                System.err.println("[WARN] Config resource not found: " + resourceName + " — using default settings.");
            }
        } catch (IOException e) {
            System.err.println("[WARN] Failed to load config: " + resourceName + " — using default settings. (" + e.getMessage() + ")");
        }
    }

    private String get(String key) {
        String sys = System.getProperty(key);
        if (sys != null && !sys.isBlank()) return sys.trim();

        String val = props.getProperty(key);
        return (val == null) ? null : val.trim();
    }

    private String getOrDefault(String key, String def) {
        String v = get(key);
        return (v == null || v.isBlank()) ? def : v;
    }

    private long getLongOrDefault(String key, long def) {
        String v = get(key);
        if (v == null || v.isBlank()) return def;
        try {
            long parsed = Long.parseLong(v);
            return parsed > 0 ? parsed : def;
        } catch (NumberFormatException e) {
            System.err.println("[WARN] Invalid long for " + key + ": " + v + " — using default " + def);
            return def;
        }
    }

    private int getIntOrDefault(String key, int def) {
        String v = get(key);
        if (v == null || v.isBlank()) return def;
        try {
            int parsed = Integer.parseInt(v);
            return parsed > 0 ? parsed : def;
        } catch (NumberFormatException e) {
            System.err.println("[WARN] Invalid int for " + key + ": " + v + " — using default " + def);
            return def;
        }
    }

    @Override
    public String baseUrl() {
        return getOrDefault("app.baseUrl", DEFAULT_BASE_URL);
    }

    @Override
    public long ttlSeconds() {
        return getLongOrDefault("app.ttlSeconds", DEFAULT_TTL_SECONDS);
    }

    @Override
    public int defaultMaxClicks() {
        return getIntOrDefault("app.defaultMaxClicks", DEFAULT_DEFAULT_MAX_CLICKS);
    }

    @Override
    public long cleanupIntervalSeconds() {
        return getLongOrDefault("app.cleanupIntervalSeconds", DEFAULT_CLEANUP_INTERVAL_SECONDS);
    }

    @Override
    public Path userUuidFile() {
        String raw = getOrDefault("app.userUuidFile", DEFAULT_USER_UUID_FILE);
        Path p = Path.of(raw);
        if (p.isAbsolute()) return p;

        String home = System.getProperty("user.home");
        return Path.of(home).resolve(p);
    }
}