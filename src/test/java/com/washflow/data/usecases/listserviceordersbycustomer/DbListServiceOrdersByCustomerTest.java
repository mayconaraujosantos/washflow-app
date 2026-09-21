package com.washflow.data.usecases.listserviceordersbycustomer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.db.LoadServiceOrdersByCustomerIdRepository;
import com.washflow.data.protocols.db.LoadUserByIdRepository;
import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.entities.ServiceOrderStatus;
import com.washflow.domain.entities.User;
import com.washflow.domain.entities.UserProfile;
import com.washflow.domain.errors.UserNotFoundError;
import com.washflow.domain.usecases.ListServiceOrdersByCustomer;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Same payoff as {@code DbListVehiclesByCustomerTest}: exercised here with plain Mockito doubles,
 * no Postgres/JDBI/Testcontainers involved at all.
 */
@ExtendWith(MockitoExtension.class)
class DbListServiceOrdersByCustomerTest {

  @Mock private LoadUserByIdRepository loadUserByIdRepository;
  @Mock private LoadServiceOrdersByCustomerIdRepository loadServiceOrdersByCustomerIdRepository;

  private DbListServiceOrdersByCustomer sut;
  private ListServiceOrdersByCustomer.Params params;
  private User customer;

  @BeforeEach
  void setUp() {
    sut =
        new DbListServiceOrdersByCustomer(
            loadUserByIdRepository, loadServiceOrdersByCustomerIdRepository);

    UUID customerId = UUID.randomUUID();
    params = new ListServiceOrdersByCustomer.Params(customerId);
    customer =
        new User(customerId, "Cliente Demo", "11999990000", UserProfile.CUSTOMER, Instant.now());
  }

  @Test
  void returnsCustomerServiceOrdersWhenCustomerExists() throws Exception {
    Instant now = Instant.now();
    List<ServiceOrder> serviceOrders =
        List.of(
            new ServiceOrder(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                null,
                ServiceOrderStatus.SCHEDULED,
                now,
                now,
                now));
    when(loadUserByIdRepository.loadById(params.customerId())).thenReturn(Optional.of(customer));
    when(loadServiceOrdersByCustomerIdRepository.loadByCustomerId(params.customerId()))
        .thenReturn(serviceOrders);

    List<ServiceOrder> result = sut.list(params);

    assertEquals(serviceOrders, result);
  }

  @Test
  void throwsUserNotFoundWhenCustomerDoesNotExist() {
    when(loadUserByIdRepository.loadById(params.customerId())).thenReturn(Optional.empty());

    assertThrows(UserNotFoundError.class, () -> sut.list(params));
    verifyNoInteractions(loadServiceOrdersByCustomerIdRepository);
  }
}
