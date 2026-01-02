import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import org.com.url_shortener.core.repository.UserUuidStore;
import org.com.url_shortener.services.UserService;
import org.junit.jupiter.api.Test;

class UserServiceTest {

  static class InMemoryUserUuidStore implements UserUuidStore {
    private String value;

    @Override
    public Optional<String> load() {
      return Optional.ofNullable(value);
    }

    @Override
    public String saveNew(String uuid) {
      this.value = uuid;
      return uuid;
    }
  }

  @Test
  void getOrCreateUserUuid_createsAndPersists_whenMissing() {
    InMemoryUserUuidStore store = new InMemoryUserUuidStore();
    UserService svc = new UserService(store);

    assertTrue(store.load().isEmpty());

    String u1 = svc.getOrCreateUserUuid();

    assertNotNull(u1);
    assertTrue(store.load().isPresent());
    assertEquals(u1, store.load().get());

    String u2 = svc.getOrCreateUserUuid();
    assertEquals(u1, u2);
  }

  @Test
  void createNewUserAndSwitch_generatesDifferentUuid_andPersists() {
    InMemoryUserUuidStore store = new InMemoryUserUuidStore();
    UserService svc = new UserService(store);

    String first = svc.getOrCreateUserUuid();
    String second = svc.createNewUserAndSwitch();

    assertNotNull(first);
    assertNotNull(second);
    assertNotEquals(first, second);
    assertEquals(second, store.load().orElseThrow());
  }

  @Test
  void switchTo_setsProvidedUuid_ifValid() {
    InMemoryUserUuidStore store = new InMemoryUserUuidStore();
    UserService svc = new UserService(store);

    String uuid = "123e4567-e89b-12d3-a456-426614174000";
    String switched = svc.switchTo(uuid);

    assertEquals(uuid, switched);
    assertEquals(uuid, store.load().orElseThrow());
  }

  @Test
  void switchTo_throwsOnInvalidUuid() {
    InMemoryUserUuidStore store = new InMemoryUserUuidStore();
    UserService svc = new UserService(store);

    assertThrows(IllegalArgumentException.class, () -> svc.switchTo("not-a-uuid"));
  }
}
