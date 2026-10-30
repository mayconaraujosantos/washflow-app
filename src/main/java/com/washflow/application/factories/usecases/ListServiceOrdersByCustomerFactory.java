package com.washflow.application.factories.usecases;

import com.washflow.data.usecases.listserviceordersbycustomer.DbListServiceOrdersByCustomer;
import com.washflow.domain.usecases.ListServiceOrdersByCustomer;
import com.washflow.infra.db.jdbi.ServiceOrderJdbiRepository;
import com.washflow.infra.db.jdbi.UserJdbiRepository;
import org.jdbi.v3.core.Jdbi;

/**
 * Swapping JDBI for Hibernate later means changing only the {@code new *JdbiRepository(...)} calls
 * here - {@code DbListServiceOrdersByCustomer} depends on the {@code data.protocols.db} interfaces,
 * not on these concrete classes.
 */
public final class ListServiceOrdersByCustomerFactory {

  private ListServiceOrdersByCustomerFactory() {}

  public static ListServiceOrdersByCustomer make(Jdbi jdbi) {
    return new DbListServiceOrdersByCustomer(
        new UserJdbiRepository(jdbi), new ServiceOrderJdbiRepository(jdbi));
  }
}
