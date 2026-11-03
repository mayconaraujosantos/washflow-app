package com.washflow.application.factories.usecases;

import com.washflow.application.AppConfig;
import com.washflow.data.usecases.establishment.DbManageEstablishment;
import com.washflow.data.usecases.establishment.DbSetupEstablishment;
import com.washflow.domain.usecases.ManageEstablishment;
import com.washflow.domain.usecases.SetupEstablishment;
import com.washflow.infra.db.jdbi.EstablishmentJdbiRepository;
import com.washflow.infra.db.jdbi.UserJdbiRepository;
import com.washflow.infra.security.BCryptPasswordHasher;
import java.time.Clock;
import org.jdbi.v3.core.Jdbi;

public final class EstablishmentFactory {

  private EstablishmentFactory() {}

  public static SetupEstablishment setup(Jdbi jdbi, AppConfig appConfig) {
    UserJdbiRepository users = new UserJdbiRepository(jdbi);
    return new DbSetupEstablishment(
        new EstablishmentJdbiRepository(jdbi),
        users,
        users,
        new BCryptPasswordHasher(),
        appConfig.setupCode(),
        appConfig.isProduction(),
        Clock.systemUTC());
  }

  public static ManageEstablishment manage(Jdbi jdbi) {
    return new DbManageEstablishment(new EstablishmentJdbiRepository(jdbi));
  }
}
