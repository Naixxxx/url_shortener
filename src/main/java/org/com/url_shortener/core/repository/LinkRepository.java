package org.com.url_shortener.core.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import org.com.url_shortener.core.models.ShortLink;

public interface LinkRepository {
  Optional<ShortLink> findByCode(String code);

  boolean existsByCode(String code);

  void save(ShortLink link);

  Optional<ShortLink> update(String code, UnaryOperator<ShortLink> updater);

  void deleteByCode(String code);

  List<ShortLink> findByOwner(String ownerUuid);

  List<ShortLink> findExpired(Instant now);
}
