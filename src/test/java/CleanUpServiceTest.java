import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.com.url_shortener.core.models.ShortLink;
import org.com.url_shortener.core.repository.LinkRepository;
import org.com.url_shortener.infra.InMemoryLinkRepository;
import org.com.url_shortener.services.CleanupService;
import org.com.url_shortener.services.NotificationService;
import org.junit.jupiter.api.Test;

class CleanupServiceTest {

  static class CapturingNotifier implements NotificationService {
    static record Event(String type, String owner, String code) {}

    final List<Event> events = new ArrayList<>();

    @Override
    public void notifyLimitReached(String ownerUuid, String code) {
      events.add(new Event("LIMIT", ownerUuid, code));
    }

    @Override
    public void notifyExpired(String ownerUuid, String code) {
      events.add(new Event("EXPIRED", ownerUuid, code));
    }
  }

  @Test
  void cleanupExpired_deletesOnlyExpired_andNotifiesOwner() {
    LinkRepository repo = new InMemoryLinkRepository();
    CapturingNotifier notifier = new CapturingNotifier();

    Instant now = Instant.parse("2025-01-01T00:00:10Z");
    Clock clock = Clock.fixed(now, ZoneOffset.UTC);

    CleanupService cleanup = new CleanupService(repo, notifier, clock);

    ShortLink expired =
        new ShortLink(
            "EXP12345",
            "ownerA",
            "https://example.com/a",
            now.minusSeconds(100),
            now.minusSeconds(1),
            10,
            0,
            false);

    ShortLink active =
        new ShortLink(
            "ACT12345",
            "ownerB",
            "https://example.com/b",
            now.minusSeconds(5),
            now.plusSeconds(100),
            10,
            0,
            false);

    repo.save(expired);
    repo.save(active);

    int removed = cleanup.cleanupExpired();

    assertEquals(1, removed, "should remove exactly 1 expired link");
    assertTrue(repo.findByCode("EXP12345").isEmpty(), "expired link should be deleted");
    assertTrue(repo.findByCode("ACT12345").isPresent(), "active link must remain");

    assertEquals(1, notifier.events.size(), "should notify exactly once");
    assertEquals("EXPIRED", notifier.events.get(0).type());
    assertEquals("ownerA", notifier.events.get(0).owner());
    assertEquals("EXP12345", notifier.events.get(0).code());
  }

  @Test
  void cleanupExpired_returnsZero_whenNothingExpired() {
    LinkRepository repo = new InMemoryLinkRepository();
    CapturingNotifier notifier = new CapturingNotifier();

    Instant now = Instant.parse("2025-01-01T00:00:10Z");
    Clock clock = Clock.fixed(now, ZoneOffset.UTC);

    CleanupService cleanup = new CleanupService(repo, notifier, clock);

    ShortLink active =
        new ShortLink(
            "ACT12345",
            "ownerB",
            "https://example.com/b",
            now.minusSeconds(5),
            now.plusSeconds(100),
            10,
            0,
            false);

    repo.save(active);

    int removed = cleanup.cleanupExpired();

    assertEquals(0, removed);
    assertTrue(repo.findByCode("ACT12345").isPresent());
    assertTrue(notifier.events.isEmpty());
  }
}
