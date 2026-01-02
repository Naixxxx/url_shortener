package org.com.url_shortener.core.repository;

import java.util.Optional;

public interface UserUuidStore {
    Optional<String> load();
    String saveNew(String uuid);
}