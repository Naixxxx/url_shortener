package org.com.url_shortener;

import java.time.Clock;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.com.url_shortener.cli.CommandLoop;
import org.com.url_shortener.core.repository.LinkRepository;
import org.com.url_shortener.core.repository.UserUuidStore;
import org.com.url_shortener.infra.FileUserUuidStore;
import org.com.url_shortener.infra.InMemoryLinkRepository;
import org.com.url_shortener.infra.cfg.AppConfig;
import org.com.url_shortener.infra.cfg.PropertiesAppConfig;
import org.com.url_shortener.services.*;

public final class Main {
  public static void main(String[] args) {
    AppConfig config = new PropertiesAppConfig("application.properties");

    LinkRepository linkRepository = new InMemoryLinkRepository();
    UserUuidStore uuidStore = new FileUserUuidStore(config.userUuidFile());

    NotificationService notifier = new ConsoleNotificationService();
    Clock clock = Clock.systemUTC();

    UserService userService = new UserService(uuidStore);
    CodeGenerator codeGenerator = new CodeGenerator(linkRepository);
    LinkService linkService =
        new LinkService(linkRepository, config, codeGenerator, notifier, clock);
    var cleanupService = new CleanupService(linkRepository, notifier, clock);

    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    scheduler.scheduleAtFixedRate(
        () -> {
          try {
            cleanupService.cleanupExpired();
          } catch (Exception ignored) {
          }
        },
        config.cleanupIntervalSeconds(),
        config.cleanupIntervalSeconds(),
        TimeUnit.SECONDS);

    try {
      new CommandLoop(config, userService, linkService, cleanupService).run();
    } finally {
      scheduler.shutdown();
    }
  }
}
