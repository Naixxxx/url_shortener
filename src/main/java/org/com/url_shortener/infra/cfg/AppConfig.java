package org.com.url_shortener.infra.cfg;

import java.nio.file.Path;

public interface AppConfig {
    String baseUrl();
    long ttlSeconds();
    int defaultMaxClicks();
    long cleanupIntervalSeconds();
    Path userUuidFile();
}