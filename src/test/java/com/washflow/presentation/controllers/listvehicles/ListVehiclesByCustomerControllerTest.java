package com.washflow.presentation.controllers.listvehiclesbycustomer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.domain.entities.Vehicle;
import com.washflow.domain.usecases.ListVehiclesByCustomer;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListVehiclesByCustomerControllerTest {

  @Mock private ListVehiclesByCustomer listVehiclesByCustomer;

  private ListVehiclesByCustomerController sut;
  private UUID customerId;

  @BeforeEach
  void setUp() {
    sut = new ListVehiclesByCustomerController(listVehiclesByCustomer);
    customerId = UUID.randomUUID();
  }

  @Test
  void listsVehiclesAndReturnsOk() throws Exception {
    HttpRequest request = new HttpRequest(Map.of(), Map.of("id", customerId.toString()), Map.of());

    List<Vehicle> vehicles =
        List.of(new Vehicle(UUID.randomUUID(), customerId, "ABC1D23", "Honda Civic", "Prata"));
    when(listVehiclesByCustomer.list(any(ListVehiclesByCustomer.Params.class)))
        .thenReturn(vehicles);

    HttpResponse response = sut.handle(request);

    assertEquals(200, response.statusCode());
    verify(listVehiclesByCustomer).list(any(ListVehiclesByCustomer.Params.class));
  }

  @Test
  void returnsBadRequestWhenCustomerIdIsInvalid() {
    HttpRequest request = new HttpRequest(Map.of(), Map.of("id", "not-a-uuid"), Map.of());

    HttpResponse response = sut.handle(request);

    assertEquals(400, response.statusCode());
  }

  @Test
  void returnsEmptyListWhenNoVehicles() throws Exception {
    HttpRequest request = new HttpRequest(Map.of(), Map.of("id", customerId.toString()), Map.of());

    when(listVehiclesByCustomer.list(any(ListVehiclesByCustomer.Params.class)))
        .thenReturn(List.of());

    HttpResponse response = sut.handle(request);

    assertEquals(200, response.statusCode());
  }
}
