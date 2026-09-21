package com.washflow.application.factories.usecases;

import com.washflow.data.usecases.getserviceorderbyid.DbGetServiceOrderById;
import com.washflow.domain.usecases.GetServiceOrderById;
import com.washflow.infra.db.jdbi.ServiceOrderJdbiRepository;
import org.jdbi.v3.core.Jdbi;

/**
 * Swapping JDBI for Hibernate later means changing only the {@code new
 * ServiceOrderJdbiRepository(...)} call here - {@code DbGetServiceOrderById} depends on the {@code
 * data.protocols.db} interfaces, not on this concrete class.
 */
public final class GetServiceOrderByIdFactory {

  private GetServiceOrderByIdFactory() {}

  public static GetServiceOrderById make(Jdbi jdbi) {
    return new DbGetServiceOrderById(new ServiceOrderJdbiRepository(jdbi));
  }
}
