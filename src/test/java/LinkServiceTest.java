import org.com.url_shortener.core.exceptions.*;
import org.com.url_shortener.core.models.ShortLink;
import org.com.url_shortener.core.repository.LinkRepository;
import org.com.url_shortener.infra.InMemoryLinkRepository;
import org.com.url_shortener.infra.cfg.AppConfig;
import org.com.url_shortener.services.CodeGenerator;
import org.com.url_shortener.services.LinkService;
import org.com.url_shortener.services.NotificationService;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class LinkServiceTest {

    static class TestConfig implements AppConfig {
        @Override public String baseUrl() { return "clck.ru"; }
        @Override public long ttlSeconds() { return 10; }
        @Override public int defaultMaxClicks() { return 2; }
        @Override public long cleanupIntervalSeconds() { return 1; }
        @Override public Path userUuidFile() { return Path.of("build/tmp/user.uuid"); }
    }

    static class TestNotifier implements NotificationService {
        int expired = 0;
        int limit = 0;
        @Override public void notifyLimitReached(String ownerUuid, String code) { limit++; }
        @Override public void notifyExpired(String ownerUuid, String code) { expired++; }
    }

    @Test
    void differentUsers_getDifferentCodes_forSameUrl() {
        LinkRepository repo = new InMemoryLinkRepository();
        TestNotifier notifier = new TestNotifier();
        Clock clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);

        LinkService svc = new LinkService(repo, new TestConfig(), new CodeGenerator(repo), notifier, clock);

        ShortLink a = svc.create("u1", "https://example.com/a", 5);
        ShortLink b = svc.create("u2", "https://example.com/a", 5);

        assertNotEquals(a.getCode(), b.getCode());
    }

    @Test
    void limit_blocksAfterReached_andNotifiesOnce() {
        LinkRepository repo = new InMemoryLinkRepository();
        TestNotifier notifier = new TestNotifier();
        Clock clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);

        LinkService svc = new LinkService(repo, new TestConfig(), new CodeGenerator(repo), notifier, clock);

        ShortLink l = svc.create("u1", "https://example.com", 2);

        assertDoesNotThrow(() -> svc.resolveForRedirect(l.getCode()));
        assertDoesNotThrow(() -> svc.resolveForRedirect(l.getCode()));
        assertEquals(1, notifier.limit);

        assertThrows(LinkLimitReachedException.class, () -> svc.resolveForRedirect(l.getCode())); // блок
        assertEquals(1, notifier.limit);
    }

    @Test
    void ttl_expires_andDeletes_onResolve() {
        LinkRepository repo = new InMemoryLinkRepository();
        TestNotifier notifier = new TestNotifier();

        Instant t0 = Instant.parse("2025-01-01T00:00:00Z");
        Clock clock0 = Clock.fixed(t0, ZoneOffset.UTC);
        LinkService svc0 = new LinkService(repo, new TestConfig(), new CodeGenerator(repo), notifier, clock0);

        ShortLink l = svc0.create("u1", "https://example.com", 5);

        Clock clock1 = Clock.fixed(t0.plusSeconds(11), ZoneOffset.UTC);
        LinkService svc1 = new LinkService(repo, new TestConfig(), new CodeGenerator(repo), notifier, clock1);

        assertThrows(LinkExpiredException.class, () -> svc1.resolveForRedirect(l.getCode()));
        assertEquals(1, notifier.expired);
        assertTrue(repo.findByCode(l.getCode()).isEmpty());
    }

    @Test
    void owner_canDelete_nonOwner_cannot() {
        LinkRepository repo = new InMemoryLinkRepository();
        TestNotifier notifier = new TestNotifier();
        Clock clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);

        LinkService svc = new LinkService(repo, new TestConfig(), new CodeGenerator(repo), notifier, clock);
        ShortLink l = svc.create("u1", "https://example.com", 5);

        assertThrows(AccessDeniedException.class, () -> svc.delete("u2", l.getCode()));
        assertDoesNotThrow(() -> svc.delete("u1", l.getCode()));
    }
}