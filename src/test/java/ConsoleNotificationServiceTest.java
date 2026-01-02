import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.com.url_shortener.services.ConsoleNotificationService;
import org.junit.jupiter.api.Test;

class ConsoleNotificationServiceTest {

  @Test
  void notifyLimitReached_printsToStderr() {
    PrintStream originalErr = System.err;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    System.setErr(new PrintStream(baos, true, StandardCharsets.UTF_8));

    try {
      ConsoleNotificationService svc = new ConsoleNotificationService();
      svc.notifyLimitReached("user-1", "CODE12345");
    } catch (Exception e) {
      fail("should not throw: " + e.getMessage());
    } finally {
      System.setErr(originalErr);
    }

    String out = baos.toString(StandardCharsets.UTF_8);
    assertTrue(out.contains("[NOTIFY]"), "should contain NOTIFY prefix");
    assertTrue(out.contains("лимит переходов исчерпан"), "should contain message about limit");
    assertTrue(out.contains("owner=user-1"), "should mention owner");
    assertTrue(out.contains("link=CODE12345"), "should mention link code");
  }

  @Test
  void notifyExpired_printsToStderr() {
    PrintStream originalErr = System.err;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    System.setErr(new PrintStream(baos, true, StandardCharsets.UTF_8));

    try {
      ConsoleNotificationService svc = new ConsoleNotificationService();
      svc.notifyExpired("user-2", "EXP00001");
    } finally {
      System.setErr(originalErr);
    }

    String out = baos.toString(StandardCharsets.UTF_8);
    assertTrue(out.contains("[NOTIFY]"));
    assertTrue(out.contains("время жизни истекло"), "should contain message about ttl");
    assertTrue(out.contains("owner=user-2"));
    assertTrue(out.contains("link=EXP00001"));
  }
}
