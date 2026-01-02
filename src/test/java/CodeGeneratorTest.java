import org.com.url_shortener.core.models.ShortLink;
import org.com.url_shortener.core.repository.LinkRepository;
import org.com.url_shortener.services.CodeGenerator;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.*;

class CodeGeneratorTest {

    @Test
    void generateUniqueCode_returnsBase62Length8() {
        LinkRepository repo = new LinkRepository() {
            @Override public Optional<ShortLink> findByCode(String code) { throw new UnsupportedOperationException(); }
            @Override public boolean existsByCode(String code) { return false; }
            @Override public void save(ShortLink link) { throw new UnsupportedOperationException(); }
            @Override public Optional<ShortLink> update(String code, UnaryOperator<ShortLink> updater) { throw new UnsupportedOperationException(); }
            @Override public void deleteByCode(String code) { throw new UnsupportedOperationException(); }
            @Override public List<ShortLink> findByOwner(String ownerUuid) { throw new UnsupportedOperationException(); }
            @Override public List<ShortLink> findExpired(Instant now) { throw new UnsupportedOperationException(); }
        };

        CodeGenerator gen = new CodeGenerator(repo);
        String code = gen.generateUniqueCode();

        assertNotNull(code);
        assertEquals(8, code.length());
        assertTrue(code.matches("^[0-9A-Za-z]{8}$"), "code must be base62 [0-9A-Za-z]{8}");
    }

    @Test
    void generateUniqueCode_retriesWhenRepositoryReportsCollision() {
        AtomicInteger existsCalls = new AtomicInteger(0);

        LinkRepository repo = new LinkRepository() {
            @Override public Optional<ShortLink> findByCode(String code) { throw new UnsupportedOperationException(); }

            @Override
            public boolean existsByCode(String code) {
                int c = existsCalls.incrementAndGet();
                return c <= 3;
            }

            @Override public void save(ShortLink link) { throw new UnsupportedOperationException(); }
            @Override public Optional<ShortLink> update(String code, UnaryOperator<ShortLink> updater) { throw new UnsupportedOperationException(); }
            @Override public void deleteByCode(String code) { throw new UnsupportedOperationException(); }
            @Override public List<ShortLink> findByOwner(String ownerUuid) { throw new UnsupportedOperationException(); }
            @Override public List<ShortLink> findExpired(Instant now) { throw new UnsupportedOperationException(); }
        };

        CodeGenerator gen = new CodeGenerator(repo);
        String code = gen.generateUniqueCode();

        assertEquals(4, existsCalls.get(), "should retry until repo says 'no collision'");
        assertNotNull(code);
        assertTrue(code.matches("^[0-9A-Za-z]{8}$"));
    }

    @Test
    void generateUniqueCode_throwsIfAllAttemptsCollide() {
        LinkRepository repo = new LinkRepository() {
            @Override public Optional<ShortLink> findByCode(String code) { throw new UnsupportedOperationException(); }
            @Override public boolean existsByCode(String code) { return true; } // всегда "занято"
            @Override public void save(ShortLink link) { throw new UnsupportedOperationException(); }
            @Override public Optional<ShortLink> update(String code, UnaryOperator<ShortLink> updater) { throw new UnsupportedOperationException(); }
            @Override public void deleteByCode(String code) { throw new UnsupportedOperationException(); }
            @Override public List<ShortLink> findByOwner(String ownerUuid) { throw new UnsupportedOperationException(); }
            @Override public List<ShortLink> findExpired(Instant now) { throw new UnsupportedOperationException(); }
        };

        CodeGenerator gen = new CodeGenerator(repo);

        assertThrows(IllegalStateException.class, gen::generateUniqueCode);
    }
}