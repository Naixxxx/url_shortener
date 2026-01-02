package org.com.url_shortener.services;

public interface NotificationService {
    void notifyLimitReached(String ownerUuid, String code);
    void notifyExpired(String ownerUuid, String code);
}