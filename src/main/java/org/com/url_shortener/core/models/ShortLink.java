package org.com.url_shortener.core.models;

import java.time.Instant;
import java.util.Objects;

public final class ShortLink {
    private final String code;
    private final String ownerUuid;
    private final String originalUrl;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final int maxClicks;
    private final int clicksUsed;
    private final boolean limitNotified;

    public ShortLink(
            String code,
            String ownerUuid,
            String originalUrl,
            Instant createdAt,
            Instant expiresAt,
            int maxClicks,
            int clicksUsed,
            boolean limitNotified
    ) {
        this.code = Objects.requireNonNull(code);
        this.ownerUuid = Objects.requireNonNull(ownerUuid);
        this.originalUrl = Objects.requireNonNull(originalUrl);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.expiresAt = Objects.requireNonNull(expiresAt);

        if (maxClicks <= 0) throw new IllegalArgumentException("maxClicks must be > 0");
        if (clicksUsed < 0) throw new IllegalArgumentException("clicksUsed must be >= 0");

        this.maxClicks = maxClicks;
        this.clicksUsed = clicksUsed;
        this.limitNotified = limitNotified;
    }

    public String getCode() {
        return code;
    }

    public String getOwnerUuid() {
        return ownerUuid;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public int getMaxClicks() {
        return maxClicks;
    }

    public int getClicksUsed() {
        return clicksUsed;
    }

    public boolean isLimitNotified() {
        return limitNotified;
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public boolean isLimitReached() {
        return clicksUsed >= maxClicks;
    }

    public ShortLink withClicksUsed(int newClicksUsed, boolean newLimitNotified) {
        return new ShortLink(
                code, ownerUuid, originalUrl, createdAt, expiresAt,
                maxClicks, newClicksUsed, newLimitNotified
        );
    }

    public ShortLink withMaxClicks(int newMaxClicks) {
        return new ShortLink(
                code, ownerUuid, originalUrl, createdAt, expiresAt,
                newMaxClicks, clicksUsed, limitNotified
        );
    }
}
