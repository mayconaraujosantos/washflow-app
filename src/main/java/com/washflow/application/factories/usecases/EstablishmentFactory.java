package com.washflow.application.factories.usecases;

import com.washflow.application.AppConfig;
import com.washflow.data.usecases.establishment.DbManageEstablishment;
import com.washflow.data.usecases.establishment.DbSetupEstablishment;
import com.washflow.data.usecases.platform.DbManagePlatform;
import com.washflow.domain.usecases.ManageEstablishment;
import com.washflow.domain.usecases.ManagePlatform;
import com.washflow.domain.usecases.SetupEstablishment;
import com.washflow.infra.db.jdbi.EstablishmentJdbiRepository;
import com.washflow.infra.db.jdbi.PlatformJdbiRepository;
import com.washflow.infra.security.BCryptPasswordHasher;
import java.time.Clock;
import org.jdbi.v3.core.Jdbi;

public final class EstablishmentFactory {

  private EstablishmentFactory() {}

  public static SetupEstablishment setup(Jdbi jdbi, AppConfig appConfig) {
    return new DbSetupEstablishment(
        new PlatformJdbiRepository(jdbi),
        new BCryptPasswordHasher(),
        appConfig.setupCode(),
        appConfig.isProduction(),
        Clock.systemUTC());
  }

  public static ManagePlatform platform(Jdbi jdbi) {
    return new DbManagePlatform(
        new PlatformJdbiRepository(jdbi), new BCryptPasswordHasher(), Clock.systemUTC());
  }

  public static ManageEstablishment manage(Jdbi jdbi) {
    return new DbManageEstablishment(new EstablishmentJdbiRepository(jdbi));
  }
}
