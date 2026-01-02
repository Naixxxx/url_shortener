package org.com.url_shortener.cli;

import java.awt.Desktop;
import java.net.URI;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import org.com.url_shortener.core.exceptions.*;
import org.com.url_shortener.core.models.ShortLink;
import org.com.url_shortener.infra.cfg.AppConfig;
import org.com.url_shortener.services.CleanupService;
import org.com.url_shortener.services.LinkService;
import org.com.url_shortener.services.UserService;

public final class CommandLoop {
  private final AppConfig config;
  private final UserService userService;
  private final LinkService linkService;
  private final CleanupService cleanupService;

  private String ownerUuid;

  public CommandLoop(
      AppConfig config,
      UserService userService,
      LinkService linkService,
      CleanupService cleanupService) {
    this.config = config;
    this.userService = userService;
    this.linkService = linkService;
    this.cleanupService = cleanupService;
  }

  public void run() {
    ownerUuid = userService.getOrCreateUserUuid();

    System.out.println("URL Shortener CLI");
    System.out.println("Ваш UUID: " + ownerUuid);
    System.out.println(
        "TTL (сек): " + config.ttlSeconds() + ", default maxClicks: " + config.defaultMaxClicks());
    System.out.println("Введите команду. help — список команд.\n");

    try (Scanner sc = new Scanner(System.in)) {
      while (true) {
        cleanupService.cleanupExpired();

        System.out.print("> ");
        if (!sc.hasNextLine()) break;

        String line = sc.nextLine().trim();
        if (line.isBlank()) continue;

        try {
          if (handle(line)) break;
        } catch (InvalidUrlException | InvalidLimitException e) {
          System.out.println("Ошибка: " + e.getMessage());
        } catch (AccessDeniedException e) {
          System.out.println("Доступ запрещён: " + e.getMessage());
        } catch (LinkNotFoundException e) {
          System.out.println("Не найдено: " + e.getMessage());
        } catch (LinkExpiredException | LinkLimitReachedException e) {
          System.out.println("Недоступно: " + e.getMessage());
        } catch (Exception e) {
          System.out.println("Неожиданная ошибка: " + e.getMessage());
        }
      }
    }
    System.out.println("Пока!");
  }

  private boolean handle(String line) throws Exception {
    String[] parts = line.split("\\s+");
    String cmd = parts[0].toLowerCase(Locale.ROOT);

    switch (cmd) {
      case "help" -> {
        printHelp();
        return false;
      }
      case "exit", "quit" -> {
        return true;
      }
      case "whoami" -> {
        System.out.println("UUID: " + ownerUuid);
        return false;
      }
      case "create" -> {
        if (parts.length < 2) {
          System.out.println("Использование: create <url> [maxClicks]");
          return false;
        }
        String url = parts[1];
        Integer maxClicks = null;
        if (parts.length >= 3) maxClicks = Integer.parseInt(parts[2]);

        ShortLink link = linkService.create(ownerUuid, url, maxClicks);
        System.out.println("OK: " + formatShort(link.getCode()));
        System.out.println(
            "code="
                + link.getCode()
                + " maxClicks="
                + link.getMaxClicks()
                + " expiresAt="
                + link.getExpiresAt());
        return false;
      }
      case "open" -> {
        if (parts.length < 2) {
          System.out.println("Использование: open <code|shortUrl>");
          return false;
        }
        String code = normalizeCode(parts[1]);
        String url = linkService.resolveForRedirect(code);

        System.out.println("Открываю: " + url);
        openInBrowser(url);
        return false;
      }
      case "user" -> {
        if (parts.length == 1) {
          System.out.println("Текущий UUID: " + ownerUuid);
          System.out.println("Подкоманды: user new | user use <uuid>");
          return false;
        }
        String sub = parts[1].toLowerCase(Locale.ROOT);
        switch (sub) {
          case "new" -> {
            ownerUuid = userService.createNewUserAndSwitch();
            System.out.println("OK: новый пользователь. UUID = " + ownerUuid);
            return false;
          }
          case "use" -> {
            if (parts.length < 3) {
              System.out.println("Использование: user use <uuid>");
              return false;
            }
            ownerUuid = userService.switchTo(parts[2]);
            System.out.println("OK: переключились. UUID = " + ownerUuid);
            return false;
          }
          default -> {
            System.out.println("Неизвестная подкоманда user: " + sub);
            System.out.println("Использование: user new | user use <uuid>");
            return false;
          }
        }
      }
      case "logout" -> {
        ownerUuid = userService.createNewUserAndSwitch();
        System.out.println("OK: logout -> новый пользователь. UUID = " + ownerUuid);
        return false;
      }
      case "list" -> {
        List<ShortLink> links = linkService.listByOwner(ownerUuid);
        if (links.isEmpty()) {
          System.out.println("(пусто)");
          return false;
        }

        DateTimeFormatter fmt =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

        for (ShortLink l : links) {
          String status = l.isLimitReached() ? "LIMIT" : "OK";
          System.out.printf(
              "%s | %s | clicks %d/%d | exp %s | %s%n",
              l.getCode(),
              status,
              l.getClicksUsed(),
              l.getMaxClicks(),
              fmt.format(l.getExpiresAt()),
              l.getOriginalUrl());
        }
        return false;
      }
      case "delete" -> {
        if (parts.length < 2) {
          System.out.println("Использование: delete <code|shortUrl>");
          return false;
        }
        String code = normalizeCode(parts[1]);
        linkService.delete(ownerUuid, code);
        System.out.println("OK: удалено " + code);
        return false;
      }
      case "set-limit" -> {
        if (parts.length < 3) {
          System.out.println("Использование: set-limit <code|shortUrl> <newLimit>");
          return false;
        }
        String code = normalizeCode(parts[1]);
        int newLimit = Integer.parseInt(parts[2]);
        ShortLink updated = linkService.updateMaxClicks(ownerUuid, code, newLimit);
        System.out.println("OK: " + updated.getCode() + " maxClicks=" + updated.getMaxClicks());
        return false;
      }
      default -> {
        System.out.println("Неизвестная команда: " + cmd + " (help — помощь)");
        return false;
      }
    }
  }

  private String formatShort(String code) {
    return config.baseUrl() + "/" + code;
  }

  private String normalizeCode(String input) {
    String s = input.trim();
    int slash = s.lastIndexOf('/');
    if (slash >= 0 && slash + 1 < s.length()) return s.substring(slash + 1);
    return s;
  }

  private void openInBrowser(String url) {
    try {
      if (!Desktop.isDesktopSupported()) {
        System.out.println("Desktop не поддерживается. Ссылка: " + url);
        return;
      }
      Desktop.getDesktop().browse(new URI(url));
    } catch (Exception e) {
      System.out.println("Не смог открыть браузер. Ссылка: " + url);
    }
  }

  private void printHelp() {
    System.out.println(
        """
                Команды:
                  help                         показать помощь
                  whoami                       показать ваш UUID
                  user                         показать текущего пользователя
                  user new                     создать нового пользователя и переключиться
                  user use <uuid>              переключиться на существующий UUID
                  logout                       алиас к user new

                  create <url> [maxClicks]     создать короткую ссылку
                  open <code|shortUrl>         открыть оригинальный URL (учёт кликов/TTL)
                  list                         список ваших ссылок
                  delete <code|shortUrl>       удалить ссылку (только владелец)
                  set-limit <code> <newLimit>  изменить лимит (только владелец)
                  exit                         выход
                """);
  }
}
