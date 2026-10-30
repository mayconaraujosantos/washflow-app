package com.washflow.presentation.controllers.scheduleserviceorder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.entities.ServiceOrderStatus;
import com.washflow.domain.errors.InsufficientLeadTimeError;
import com.washflow.domain.errors.OutsideBusinessHoursError;
import com.washflow.domain.errors.SlotFullyBookedError;
import com.washflow.domain.errors.VehicleNotFoundError;
import com.washflow.domain.usecases.ScheduleServiceOrder;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import com.washflow.validation.protocols.Validation;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceOrderControllerTest {

  @Mock private ScheduleServiceOrder scheduleServiceOrder;
  @Mock private Validation validation;

  private ScheduleServiceOrderController sut;
  private UUID vehicleId;

  @BeforeEach
  void setUp() {
    sut = new ScheduleServiceOrderController(scheduleServiceOrder, validation);
    vehicleId = UUID.randomUUID();
  }

  @Test
  void schedulesServiceOrderAndReturnsOk() throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("vehicleId", vehicleId.toString());
    body.put("servicePriceId", 1);
    body.put("scheduledAt", Instant.now().toString());
    HttpRequest request = new HttpRequest(body, Map.of(), Map.of());

    when(validation.validate(body)).thenReturn(null);
    ServiceOrder saved =
        new ServiceOrder(
            UUID.randomUUID(),
            vehicleId,
            1,
            null,
            ServiceOrderStatus.SCHEDULED,
            Instant.now(),
            Instant.now(),
            Instant.now());
    when(scheduleServiceOrder.schedule(any(ScheduleServiceOrder.Params.class))).thenReturn(saved);

    HttpResponse response = sut.handle(request);

    assertEquals(200, response.statusCode());
    verify(scheduleServiceOrder).schedule(any(ScheduleServiceOrder.Params.class));
  }

  @Test
  void returnsBadRequestWhenValidationFails() {
    Map<String, Object> body = new HashMap<>();
    HttpRequest request = new HttpRequest(body, Map.of(), Map.of());

    when(validation.validate(body)).thenReturn(new IllegalArgumentException("missing field"));

    HttpResponse response = sut.handle(request);

    assertEquals(400, response.statusCode());
  }

  @Test
  void returnsBadRequestWhenOutsideBusinessHours() throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("vehicleId", vehicleId.toString());
    body.put("servicePriceId", 1);
    body.put("scheduledAt", Instant.now().toString());
    HttpRequest request = new HttpRequest(body, Map.of(), Map.of());

    when(validation.validate(body)).thenReturn(null);
    when(scheduleServiceOrder.schedule(any(ScheduleServiceOrder.Params.class)))
        .thenThrow(new OutsideBusinessHoursError(Instant.now()));

    HttpResponse response = sut.handle(request);

    assertEquals(400, response.statusCode());
  }

  @Test
  void returnsBadRequestWhenInsufficientLeadTime() throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("vehicleId", vehicleId.toString());
    body.put("servicePriceId", 1);
    body.put("scheduledAt", Instant.now().toString());
    HttpRequest request = new HttpRequest(body, Map.of(), Map.of());

    when(validation.validate(body)).thenReturn(null);
    when(scheduleServiceOrder.schedule(any(ScheduleServiceOrder.Params.class)))
        .thenThrow(new InsufficientLeadTimeError(Instant.now(), java.time.Duration.ofMinutes(30)));

    HttpResponse response = sut.handle(request);

    assertEquals(400, response.statusCode());
  }

  @Test
  void returnsNotFoundWhenVehicleNotFound() throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("vehicleId", vehicleId.toString());
    body.put("servicePriceId", 1);
    body.put("scheduledAt", Instant.now().toString());
    HttpRequest request = new HttpRequest(body, Map.of(), Map.of());

    when(validation.validate(body)).thenReturn(null);
    when(scheduleServiceOrder.schedule(any(ScheduleServiceOrder.Params.class)))
        .thenThrow(new VehicleNotFoundError(vehicleId));

    HttpResponse response = sut.handle(request);

    assertEquals(404, response.statusCode());
  }

  @Test
  void returnsConflictWhenSlotFullyBooked() throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("vehicleId", vehicleId.toString());
    body.put("servicePriceId", 1);
    body.put("scheduledAt", Instant.now().toString());
    HttpRequest request = new HttpRequest(body, Map.of(), Map.of());

    when(validation.validate(body)).thenReturn(null);
    when(scheduleServiceOrder.schedule(any(ScheduleServiceOrder.Params.class)))
        .thenThrow(new SlotFullyBookedError(Instant.now(), 1));

    HttpResponse response = sut.handle(request);

    assertEquals(409, response.statusCode());
  }
}
