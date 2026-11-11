package com.washflow.application.modules;

import com.washflow.application.AppConfig;
import com.washflow.data.protocols.messaging.OtpSender;
import com.washflow.data.usecases.customer.DbCustomerPasswordlessLogin;
import com.washflow.data.usecases.customer.DbCustomerRecoveryEmail;
import com.washflow.data.usecases.customer.DbListServiceOrdersByCustomer;
import com.washflow.data.usecases.customer.DbServiceOrderTicket;
import com.washflow.data.usecases.user.DbWasherBadge;
import com.washflow.domain.usecases.customer.CustomerPasswordlessLogin;
import com.washflow.domain.usecases.customer.CustomerRecoveryEmail;
import com.washflow.domain.usecases.customer.ListServiceOrdersByCustomer;
import com.washflow.domain.usecases.customer.ServiceOrderTicket;
import com.washflow.domain.usecases.user.WasherBadge;
import com.washflow.infra.db.jdbi.customer.AccessJdbiRepository;
import com.washflow.infra.db.jdbi.serviceorder.ServiceOrderJdbiRepository;
import com.washflow.infra.db.jdbi.user.PasswordResetJdbiRepository;
import com.washflow.infra.db.jdbi.user.UserJdbiRepository;
import com.washflow.infra.identity.GoogleTokeninfoVerifier;
import com.washflow.infra.messaging.EmailOtpSender;
import com.washflow.infra.messaging.LoggingOtpSender;
import com.washflow.infra.messaging.UnavailableOtpSender;
import com.washflow.infra.security.BCryptPasswordHasher;
import com.washflow.infra.security.HmacTokenHasher;
import com.washflow.presentation.controllers.customer.ListServiceOrdersByCustomerController;
import com.washflow.presentation.protocols.Controller;
import java.time.Clock;
import org.jdbi.v3.core.Jdbi;

/**
 * Composition root of the customer's orders, passwordless sign-in, public ticket and washer badge:
 * builds its use cases and controllers.
 */
public final class CustomerModule {

  private CustomerModule() {}

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
        whatsappSender(appConfig),
        emailSender(appConfig),
        new HmacTokenHasher(appConfig.tokenSecret()),
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
  private static OtpSender whatsappSender(AppConfig appConfig) {
    if ("log".equalsIgnoreCase(appConfig.otpChannel()) && !appConfig.isProduction()) {
      return new LoggingOtpSender();
    }
    return new UnavailableOtpSender();
  }

  /** E-mail codes go through the app's single e-mail channel (EMAIL_CHANNEL + SMTP_*). */
  private static OtpSender emailSender(AppConfig appConfig) {
    return new EmailOtpSender(UserModule.emailSender(appConfig));
  }

  /**
   * Swapping JDBI for Hibernate later means changing only the {@code new *JdbiRepository(...)}
   * calls here - {@code DbListServiceOrdersByCustomer} depends on the {@code data.protocols.db}
   * interfaces, not on these concrete classes.
   */
  public static ListServiceOrdersByCustomer listServiceOrdersByCustomer(Jdbi jdbi) {
    return new DbListServiceOrdersByCustomer(
        new UserJdbiRepository(jdbi), new ServiceOrderJdbiRepository(jdbi));
  }

  public static Controller listServiceOrdersByCustomerController(Jdbi jdbi) {
    return new ListServiceOrdersByCustomerController(listServiceOrdersByCustomer(jdbi));
  }

  public static CustomerRecoveryEmail recoveryEmail(Jdbi jdbi) {
    return new DbCustomerRecoveryEmail(new PasswordResetJdbiRepository(jdbi));
  }
}
