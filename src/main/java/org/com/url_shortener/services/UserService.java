package org.com.url_shortener.services;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.com.url_shortener.core.repository.UserUuidStore;

public final class UserService {
  private final UserUuidStore store;

  public UserService(UserUuidStore store) {
    this.store = Objects.requireNonNull(store);
  }

  public String getOrCreateUserUuid() {
    return store.load().orElseGet(() -> store.saveNew(UUID.randomUUID().toString()));
  }

  public String createNewUserAndSwitch() {
    return store.saveNew(UUID.randomUUID().toString());
  }

  public String switchTo(String uuid) {
    validateUuid(uuid);
    return store.saveNew(uuid);
  }

  public Optional<String> load() {
    return store.load();
  }

  private void validateUuid(String uuid) {
    try {
      UUID.fromString(uuid);
    } catch (Exception e) {
      throw new IllegalArgumentException("Невалидный UUID: " + uuid);
    }
  }
}
