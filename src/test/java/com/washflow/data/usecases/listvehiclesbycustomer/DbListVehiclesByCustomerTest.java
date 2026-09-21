package com.washflow.data.usecases.listvehiclesbycustomer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.db.LoadUserByIdRepository;
import com.washflow.data.protocols.db.LoadVehiclesByCustomerIdRepository;
import com.washflow.domain.entities.User;
import com.washflow.domain.entities.UserProfile;
import com.washflow.domain.entities.Vehicle;
import com.washflow.domain.errors.UserNotFoundError;
import com.washflow.domain.usecases.ListVehiclesByCustomer;
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
 * Same payoff as {@code DbScheduleServiceOrderTest}: exercised here with plain Mockito doubles, no
 * Postgres/JDBI/Testcontainers involved at all.
 */
@ExtendWith(MockitoExtension.class)
class DbListVehiclesByCustomerTest {

  @Mock private LoadUserByIdRepository loadUserByIdRepository;
  @Mock private LoadVehiclesByCustomerIdRepository loadVehiclesByCustomerIdRepository;

  private DbListVehiclesByCustomer sut;
  private ListVehiclesByCustomer.Params params;
  private User customer;

  @BeforeEach
  void setUp() {
    sut = new DbListVehiclesByCustomer(loadUserByIdRepository, loadVehiclesByCustomerIdRepository);

    UUID customerId = UUID.randomUUID();
    params = new ListVehiclesByCustomer.Params(customerId);
    customer =
        new User(customerId, "Cliente Demo", "11999990000", UserProfile.CUSTOMER, Instant.now());
  }

  @Test
  void returnsCustomerVehiclesWhenCustomerExists() throws Exception {
    List<Vehicle> vehicles =
        List.of(new Vehicle(UUID.randomUUID(), params.customerId(), "ABC1D23", "Onix", "Prata"));
    when(loadUserByIdRepository.loadById(params.customerId())).thenReturn(Optional.of(customer));
    when(loadVehiclesByCustomerIdRepository.loadByCustomerId(params.customerId()))
        .thenReturn(vehicles);

    List<Vehicle> result = sut.list(params);

    assertEquals(vehicles, result);
  }

  @Test
  void throwsUserNotFoundWhenCustomerDoesNotExist() {
    when(loadUserByIdRepository.loadById(params.customerId())).thenReturn(Optional.empty());

    assertThrows(UserNotFoundError.class, () -> sut.list(params));
    verifyNoInteractions(loadVehiclesByCustomerIdRepository);
  }
}
