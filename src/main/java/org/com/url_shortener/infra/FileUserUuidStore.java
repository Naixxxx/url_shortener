package org.com.url_shortener.infra;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.com.url_shortener.core.repository.UserUuidStore;

public final class FileUserUuidStore implements UserUuidStore {
  private final Path path;

  public FileUserUuidStore(Path path) {
    this.path = path;
  }

  @Override
  public Optional<String> load() {
    try {
      if (!Files.exists(path)) return Optional.empty();
      String s = Files.readString(path, StandardCharsets.UTF_8).trim();
      if (s.isBlank()) return Optional.empty();
      return Optional.of(s);
    } catch (IOException e) {
      return Optional.empty();
    }
  }

  @Override
  public String saveNew(String uuid) {
    try {
      Path dir = path.getParent();
      if (dir != null) Files.createDirectories(dir);
      Files.writeString(path, uuid, StandardCharsets.UTF_8);
      return uuid;
    } catch (IOException e) {
      throw new IllegalStateException("Failed to save user UUID to " + path, e);
    }
  }
}
