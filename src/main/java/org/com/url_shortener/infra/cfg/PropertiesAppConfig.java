package org.com.url_shortener.infra.cfg;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;

public final class PropertiesAppConfig implements AppConfig {
    private final Properties props;

    public PropertiesAppConfig(String resourceName) {
        this.props = new Properties();
        try (InputStream in = PropertiesAppConfig.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (in == null) throw new IllegalStateException("Config resource not found: " + resourceName);
            this.props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load config: " + resourceName, e);
        }
    }

    private String get(String key) {
        String sys = System.getProperty(key);
        return (sys != null && !sys.isBlank()) ? sys : props.getProperty(key);
    }

    @Override
    public String baseUrl() {
        return get("app.baseUrl");
    }

    @Override
    public long ttlSeconds() {
        return Long.parseLong(get("app.ttlSeconds"));
    }

    @Override
    public int defaultMaxClicks() {
        return Integer.parseInt(get("app.defaultMaxClicks"));
    }

    @Override
    public long cleanupIntervalSeconds() {
        return Long.parseLong(get("app.cleanupIntervalSeconds"));
    }

    @Override
    public Path userUuidFile() {
        String raw = get("app.userUuidFile");
        Path p = Path.of(raw);
        if (p.isAbsolute()) return p;

        String home = System.getProperty("user.home");
        return Path.of(home).resolve(p);
    }
}