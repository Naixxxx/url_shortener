package org.com.url_shortener.services;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.validator.routines.UrlValidator;
import org.com.url_shortener.core.exceptions.*;
import org.com.url_shortener.core.models.ShortLink;
import org.com.url_shortener.core.repository.LinkRepository;
import org.com.url_shortener.infra.cfg.AppConfig;

public final class LinkService {
  private final LinkRepository repo;
  private final AppConfig config;
  private final CodeGenerator codeGenerator;
  private final NotificationService notifier;
  private final Clock clock;

  private final UrlValidator urlValidator = new UrlValidator(new String[] {"http", "https"});

  public LinkService(
      LinkRepository repo,
      AppConfig config,
      CodeGenerator codeGenerator,
      NotificationService notifier,
      Clock clock) {
    this.repo = Objects.requireNonNull(repo);
    this.config = Objects.requireNonNull(config);
    this.codeGenerator = Objects.requireNonNull(codeGenerator);
    this.notifier = Objects.requireNonNull(notifier);
    this.clock = Objects.requireNonNull(clock);
  }

  public ShortLink create(String ownerUuid, String url, Integer maxClicksOrNull) {
    Objects.requireNonNull(ownerUuid);
    Objects.requireNonNull(url);

    if (!urlValidator.isValid(url)) {
      throw new InvalidUrlException("Невалидный URL (нужен http/https): " + url);
    }

    int maxClicks = (maxClicksOrNull != null) ? maxClicksOrNull : config.defaultMaxClicks();
    if (maxClicks <= 0) throw new InvalidLimitException("Лимит переходов должен быть > 0");

    Instant now = clock.instant();
    Instant expiresAt = now.plusSeconds(config.ttlSeconds());

    String code = codeGenerator.generateUniqueCode();

    ShortLink link = new ShortLink(code, ownerUuid, url, now, expiresAt, maxClicks, 0, false);

    repo.save(link);
    return link;
  }

  public String resolveForRedirect(String code) {
    Objects.requireNonNull(code);
    Instant now = clock.instant();

    AtomicReference<String> urlRef = new AtomicReference<>();
    AtomicReference<RuntimeException> exRef = new AtomicReference<>(null);

    repo.update(
        code,
        link -> {
          urlRef.set(link.getOriginalUrl());

          if (link.isExpired(now)) {
            notifier.notifyExpired(link.getOwnerUuid(), link.getCode());
            exRef.set(new LinkExpiredException("Ссылка истекла по времени жизни (TTL) и удалена"));
            return null;
          }

          if (link.isLimitReached()) {
            exRef.set(
                new LinkLimitReachedException("Лимит переходов исчерпан — ссылка недоступна"));
            return link;
          }

          int newClicks = link.getClicksUsed() + 1;
          boolean notifyNow = (newClicks >= link.getMaxClicks()) && !link.isLimitNotified();

          ShortLink updated = link.withClicksUsed(newClicks, link.isLimitNotified() || notifyNow);

          if (notifyNow) {
            notifier.notifyLimitReached(link.getOwnerUuid(), link.getCode());
          }

          return updated;
        });

    RuntimeException ex = exRef.get();
    if (ex != null) throw ex;

    String url = urlRef.get();
    if (url == null) throw new LinkNotFoundException("Ссылка не найдена: " + code);
    return url;
  }

  public List<ShortLink> listByOwner(String ownerUuid) {
    Objects.requireNonNull(ownerUuid);
    return repo.findByOwner(ownerUuid);
  }

  public void delete(String ownerUuid, String code) {
    Objects.requireNonNull(ownerUuid);
    Objects.requireNonNull(code);

    ShortLink link =
        repo.findByCode(code)
            .orElseThrow(() -> new LinkNotFoundException("Ссылка не найдена: " + code));

    if (!link.getOwnerUuid().equals(ownerUuid)) {
      throw new AccessDeniedException("Удалять может только владелец ссылки");
    }
    repo.deleteByCode(code);
  }

  public ShortLink updateMaxClicks(String ownerUuid, String code, int newLimit) {
    Objects.requireNonNull(ownerUuid);
    Objects.requireNonNull(code);
    if (newLimit <= 0) throw new InvalidLimitException("Новый лимит должен быть > 0");

    AtomicReference<RuntimeException> exRef = new AtomicReference<>(null);
    AtomicReference<ShortLink> updatedRef = new AtomicReference<>(null);

    repo.update(
        code,
        link -> {
          if (!link.getOwnerUuid().equals(ownerUuid)) {
            exRef.set(new AccessDeniedException("Редактировать может только владелец ссылки"));
            return link;
          }

          ShortLink updated = link.withMaxClicks(newLimit);

          if (updated.getClicksUsed() >= updated.getMaxClicks() && !updated.isLimitNotified()) {
            notifier.notifyLimitReached(updated.getOwnerUuid(), updated.getCode());
            updated = updated.withClicksUsed(updated.getClicksUsed(), true);
          }

          updatedRef.set(updated);
          return updated;
        });

    if (updatedRef.get() == null && exRef.get() == null) {
      throw new LinkNotFoundException("Ссылка не найдена: " + code);
    }
    if (exRef.get() != null) throw exRef.get();
    return updatedRef.get();
  }
}
