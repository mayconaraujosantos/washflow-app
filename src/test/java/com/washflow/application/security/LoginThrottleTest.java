package com.washflow.application.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.javalin.http.Context;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class LoginThrottleTest {

  private static final String MANAGERS = "/api/auth/managers";
  private static final String CUSTOMERS = "/api/auth/customers";

  private final MutableClock clock = new MutableClock(Instant.parse("2026-11-10T12:00:00Z"));
  private final LoginThrottle throttle = new LoginThrottle(clock);
  private final AtomicInteger runs = new AtomicInteger();

  @Test
  void anAccountIsLockedAfterRepeatedWrongPasswords() throws Exception {
    for (int i = 0; i < LoginThrottle.MAX_ACCOUNT_FAILURES; i++) {
      throttle.handle(
          "POST", login(MANAGERS, "1.1.1.1", "{\"email\":\"ana@lava.com\"}", 401), this::signIn);
    }

    Context blocked = login(MANAGERS, "1.1.1.1", "{\"email\":\"ana@lava.com\"}", 200);
    throttle.handle("POST", blocked, this::signIn);

    assertEquals(LoginThrottle.MAX_ACCOUNT_FAILURES, runs.get(), "the password is not checked");
    verify(blocked).status(429);
    verify(blocked).header("Retry-After", "900");
  }

  @Test
  void theLockIsPerAccountAndIgnoresCaseAndPhoneFormatting() throws Exception {
    for (int i = 0; i < LoginThrottle.MAX_ACCOUNT_FAILURES; i++) {
      throttle.handle(
          "POST",
          login(CUSTOMERS, "1.1.1." + i, "{\"phone\":\"(92) 99999-0000\"}", 401),
          this::signIn);
    }

    Context sameAccount = login(CUSTOMERS, "9.9.9.9", "{\"phone\":\"92999990000\"}", 200);
    throttle.handle("POST", sameAccount, this::signIn);
    verify(sameAccount).status(429);

    Context otherAccount = login(CUSTOMERS, "9.9.9.9", "{\"phone\":\"92888880000\"}", 200);
    throttle.handle("POST", otherAccount, this::signIn);
    verify(otherAccount, never()).status(429);
  }

  @Test
  void sameAccountInAnotherLavaJatoIsNotAffected() throws Exception {
    for (int i = 0; i < LoginThrottle.MAX_ACCOUNT_FAILURES; i++) {
      Context ctx = login(CUSTOMERS, "1.1.1.1", "{\"phone\":\"92999990000\"}", 401);
      when(ctx.header("X-Establishment")).thenReturn("lava-a");
      throttle.handle("POST", ctx, this::signIn);
    }

    Context other = login(CUSTOMERS, "2.2.2.2", "{\"phone\":\"92999990000\"}", 200);
    when(other.header("X-Establishment")).thenReturn("lava-b");
    throttle.handle("POST", other, this::signIn);

    verify(other, never()).status(429);
  }

  @Test
  void theLockExpires() throws Exception {
    for (int i = 0; i < LoginThrottle.MAX_ACCOUNT_FAILURES; i++) {
      throttle.handle(
          "POST", login(MANAGERS, "1.1.1.1", "{\"email\":\"ana@lava.com\"}", 401), this::signIn);
    }
    clock.advance(LoginThrottle.LOCK.plusSeconds(1));

    Context later = login(MANAGERS, "1.1.1.1", "{\"email\":\"ANA@lava.com\"}", 200);
    throttle.handle("POST", later, this::signIn);

    verify(later, never()).status(429);
  }

  @Test
  void aSuccessfulLoginClearsThePreviousFailures() throws Exception {
    for (int i = 0; i < LoginThrottle.MAX_ACCOUNT_FAILURES - 1; i++) {
      throttle.handle(
          "POST", login(MANAGERS, "1.1.1.1", "{\"email\":\"ana@lava.com\"}", 401), this::signIn);
    }
    throttle.handle(
        "POST", login(MANAGERS, "1.1.1.1", "{\"email\":\"ana@lava.com\"}", 200), this::signIn);
    throttle.handle(
        "POST", login(MANAGERS, "1.1.1.1", "{\"email\":\"ana@lava.com\"}", 401), this::signIn);

    Context next = login(MANAGERS, "1.1.1.1", "{\"email\":\"ana@lava.com\"}", 200);
    throttle.handle("POST", next, this::signIn);

    verify(next, never()).status(429);
  }

  @Test
  void wrongResetCodesCountAndANewPasswordUnlocksTheLogin() throws Exception {
    String confirm = "/api/auth/customers/password-reset/confirm";
    String ana = "{\"phone\":\"(92) 99123-4567\"}";
    for (int i = 0; i < LoginThrottle.MAX_ACCOUNT_FAILURES; i++) {
      throttle.handle("POST", login(CUSTOMERS, "1.1.1.1", ana, 401), this::signIn);
    }
    Context locked = login(CUSTOMERS, "1.1.1.1", ana, 200);
    throttle.handle("POST", locked, this::signIn);
    verify(locked).status(429);

    throttle.handle("POST", login(confirm, "1.1.1.1", ana, 200), this::signIn);

    Context afterReset = login(CUSTOMERS, "1.1.1.1", ana, 200);
    throttle.handle("POST", afterReset, this::signIn);
    verify(afterReset, never()).status(429);

    for (int i = 0; i < LoginThrottle.MAX_ACCOUNT_FAILURES; i++) {
      throttle.handle("POST", login(confirm, "2.2.2.2", ana, 401), this::signIn);
    }
    Context guessing = login(confirm, "2.2.2.2", ana, 200);
    throttle.handle("POST", guessing, this::signIn);
    verify(guessing).status(429);
  }

  @Test
  void oneIpGuessingManyAccountsIsBlocked() throws Exception {
    for (int i = 0; i < LoginThrottle.MAX_IP_FAILURES; i++) {
      throttle.handle(
          "POST",
          login(
              CUSTOMERS, "6.6.6.6", "{\"phone\":\"9299999" + String.format("%04d", i) + "\"}", 401),
          this::signIn);
    }

    Context fresh = login(CUSTOMERS, "6.6.6.6", "{\"phone\":\"92111110000\"}", 200);
    throttle.handle("POST", fresh, this::signIn);
    verify(fresh).status(429);

    Context neighbour = login(CUSTOMERS, "7.7.7.7", "{\"phone\":\"92111110000\"}", 200);
    throttle.handle("POST", neighbour, this::signIn);
    verify(neighbour, never()).status(429);
  }

  @Test
  void behindTheProxyTheClientIpComesFromTheLastForwardedHop() throws Exception {
    for (int i = 0; i < LoginThrottle.MAX_IP_FAILURES; i++) {
      Context ctx =
          login(
              CUSTOMERS,
              "10.0.0.1",
              "{\"phone\":\"9299999" + String.format("%04d", i) + "\"}",
              401);
      when(ctx.header("X-Forwarded-For")).thenReturn("spoofed-" + i + ", 6.6.6.6");
      throttle.handle("POST", ctx, this::signIn);
    }

    Context attacker = login(CUSTOMERS, "10.0.0.1", "{\"phone\":\"92111110000\"}", 200);
    when(attacker.header("X-Forwarded-For")).thenReturn("6.6.6.6");
    throttle.handle("POST", attacker, this::signIn);
    verify(attacker).status(429);

    Context otherCustomer = login(CUSTOMERS, "10.0.0.1", "{\"phone\":\"92111110000\"}", 200);
    when(otherCustomer.header("X-Forwarded-For")).thenReturn("7.7.7.7");
    throttle.handle("POST", otherCustomer, this::signIn);
    verify(otherCustomer, never()).status(429);
  }

  @Test
  void otherRoutesAndMalformedBodiesPassThrough() throws Exception {
    Context sale = login("/api/sales", "1.1.1.1", "{}", 401);
    for (int i = 0; i < 50; i++) {
      throttle.handle("POST", sale, this::signIn);
    }
    Context garbage = login(MANAGERS, "1.1.1.1", "not json", 400);
    throttle.handle("POST", garbage, this::signIn);
    Context get = login(MANAGERS, "1.1.1.1", "", 200);
    throttle.handle("GET", get, this::signIn);

    assertEquals(52, runs.get());
    verify(sale, never()).status(429);
  }

  private void signIn(Context ignored) {
    runs.incrementAndGet();
  }

  private static Context login(String path, String ip, String body, int status) {
    Context ctx = mock(Context.class, RETURNS_SELF);
    when(ctx.path()).thenReturn(path);
    when(ctx.ip()).thenReturn(ip);
    when(ctx.body()).thenReturn(body);
    when(ctx.statusCode()).thenReturn(status);
    return ctx;
  }

  private static final class MutableClock extends Clock {
    private Instant now;

    MutableClock(Instant now) {
      this.now = now;
    }

    void advance(Duration duration) {
      now = now.plus(duration);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }
}
