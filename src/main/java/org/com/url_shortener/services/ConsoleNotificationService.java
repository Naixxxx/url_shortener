package org.com.url_shortener.services;

public final class ConsoleNotificationService implements NotificationService {
  @Override
  public void notifyLimitReached(String ownerUuid, String code) {
    System.err.printf("[NOTIFY] owner=%s link=%s: лимит переходов исчерпан%n", ownerUuid, code);
  }

  @Override
  public void notifyExpired(String ownerUuid, String code) {
    System.err.printf(
        "[NOTIFY] owner=%s link=%s: время жизни истекло, ссылка удалена%n", ownerUuid, code);
  }
}
