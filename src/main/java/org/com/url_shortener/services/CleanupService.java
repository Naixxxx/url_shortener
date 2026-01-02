package org.com.url_shortener.services;

import org.com.url_shortener.core.models.ShortLink;
import org.com.url_shortener.core.repository.LinkRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class CleanupService {
    private final LinkRepository repo;
    private final NotificationService notifier;
    private final Clock clock;

    public CleanupService(LinkRepository repo, NotificationService notifier, Clock clock) {
        this.repo = Objects.requireNonNull(repo);
        this.notifier = Objects.requireNonNull(notifier);
        this.clock = Objects.requireNonNull(clock);
    }

    public int cleanupExpired() {
        Instant now = clock.instant();
        List<ShortLink> expired = repo.findExpired(now);
        for (ShortLink link : expired) {
            repo.deleteByCode(link.getCode());
            notifier.notifyExpired(link.getOwnerUuid(), link.getCode());
        }
        return expired.size();
    }
}
