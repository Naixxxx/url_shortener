package org.com.url_shortener.infra;

import org.com.url_shortener.core.models.ShortLink;
import org.com.url_shortener.core.repository.LinkRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

public final class InMemoryLinkRepository implements LinkRepository {
    private final ConcurrentHashMap<String, ShortLink> map = new ConcurrentHashMap<>();

    @Override
    public Optional<ShortLink> findByCode(String code) {
        return Optional.ofNullable(map.get(code));
    }

    @Override
    public boolean existsByCode(String code) {
        return map.containsKey(code);
    }

    @Override
    public void save(ShortLink link) {
        map.put(link.getCode(), link);
    }

    @Override
    public Optional<ShortLink> update(String code, UnaryOperator<ShortLink> updater) {
        final ShortLink[] updated = new ShortLink[1];
        map.compute(code, (k, v) -> {
            if (v == null) {
                updated[0] = null;
                return null;
            }
            ShortLink nv = updater.apply(v);
            updated[0] = nv;
            return nv;
        });
        return Optional.ofNullable(updated[0]);
    }

    @Override
    public void deleteByCode(String code) {
        map.remove(code);
    }

    @Override
    public List<ShortLink> findByOwner(String ownerUuid) {
        List<ShortLink> res = new ArrayList<>();
        for (ShortLink link : map.values()) {
            if (link.getOwnerUuid().equals(ownerUuid)) res.add(link);
        }
        return res;
    }

    @Override
    public List<ShortLink> findExpired(Instant now) {
        List<ShortLink> res = new ArrayList<>();
        for (ShortLink link : map.values()) {
            if (link.isExpired(now)) res.add(link);
        }
        return res;
    }
}