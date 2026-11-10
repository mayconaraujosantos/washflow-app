package com.washflow.application.factories.usecases.access;

import com.washflow.application.AppConfig;
import com.washflow.data.protocols.messaging.OtpSender;
import com.washflow.data.usecases.access.DbWasherBadge;
import com.washflow.data.usecases.customer.DbCustomerPasswordlessLogin;
import com.washflow.data.usecases.customer.DbServiceOrderTicket;
import com.washflow.domain.usecases.access.WasherBadge;
import com.washflow.domain.usecases.customer.CustomerPasswordlessLogin;
import com.washflow.domain.usecases.customer.ServiceOrderTicket;
import com.washflow.infra.db.jdbi.AccessJdbiRepository;
import com.washflow.infra.db.jdbi.UserJdbiRepository;
import com.washflow.infra.identity.GoogleTokeninfoVerifier;
import com.washflow.infra.messaging.LoggingOtpSender;
import com.washflow.infra.messaging.UnavailableOtpSender;
import com.washflow.infra.security.BCryptPasswordHasher;
import com.washflow.infra.security.HmacTokenHasher;
import java.time.Clock;
import org.jdbi.v3.core.Jdbi;

public final class AccessFactory {

  private AccessFactory() {}

  public static WasherBadge washerBadge(Jdbi jdbi, AppConfig appConfig) {
    UserJdbiRepository users = new UserJdbiRepository(jdbi);
    return new DbWasherBadge(
        users,
        users,
        new AccessJdbiRepository(jdbi),
        new BCryptPasswordHasher(),
        new HmacTokenHasher(appConfig.tokenSecret()),
        Clock.systemUTC());
  }

  public static ServiceOrderTicket ticket(Jdbi jdbi) {
    return new DbServiceOrderTicket(new AccessJdbiRepository(jdbi));
  }

  public static CustomerPasswordlessLogin customerLogin(Jdbi jdbi, AppConfig appConfig) {
    UserJdbiRepository users = new UserJdbiRepository(jdbi);
    AccessJdbiRepository access = new AccessJdbiRepository(jdbi);
    return new DbCustomerPasswordlessLogin(
        access,
        otpSender(appConfig),
        new HmacTokenHasher(appConfig.tokenSecret()),
        users,
        users,
        users,
        access,
        new GoogleTokeninfoVerifier(appConfig.googleClientId()),
        appConfig.publicAppUrl(),
        Clock.systemUTC());
  }

  /**
   * Only the development log channel exists today; plug a WhatsApp provider here (Twilio, Meta
   * Cloud API, Z-API...) selected by OTP_CHANNEL. The log channel is refused in production so codes
   * never end up in production logs.
   */
  static OtpSender otpSender(AppConfig appConfig) {
    if ("log".equalsIgnoreCase(appConfig.otpChannel()) && !appConfig.isProduction()) {
      return new LoggingOtpSender();
    }
    return new UnavailableOtpSender();
  }
}
