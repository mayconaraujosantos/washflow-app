package com.washflow.data.usecases.finalizeserviceorder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.db.FinalizeServiceOrderRepository;
import com.washflow.data.protocols.db.LoadServiceOrderByIdRepository;
import com.washflow.data.protocols.db.LoadServicePriceByIdRepository;
import com.washflow.domain.entities.PaymentMethod;
import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.entities.ServiceOrderStatus;
import com.washflow.domain.entities.ServicePayment;
import com.washflow.domain.entities.ServicePrice;
import com.washflow.domain.entities.WasherCommission;
import com.washflow.domain.errors.InvalidServiceOrderStatusError;
import com.washflow.domain.errors.ServiceOrderNotFoundError;
import com.washflow.domain.errors.ServicePriceNotFoundError;
import com.washflow.domain.usecases.FinalizeServiceOrder;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Same payoff as {@code DbCompleteServiceOrderTest}: exercised here with plain Mockito doubles, no
 * Postgres/JDBI/Testcontainers involved at all.
 */
@ExtendWith(MockitoExtension.class)
class DbFinalizeServiceOrderTest {

  @Mock private LoadServiceOrderByIdRepository loadServiceOrderByIdRepository;
  @Mock private LoadServicePriceByIdRepository loadServicePriceByIdRepository;
  @Mock private FinalizeServiceOrderRepository finalizeServiceOrderRepository;

  private DbFinalizeServiceOrder sut;
  private FinalizeServiceOrder.Params params;
  private ServiceOrder readyServiceOrder;
  private ServicePrice servicePrice;

  @BeforeEach
  void setUp() {
    sut =
        new DbFinalizeServiceOrder(
            loadServiceOrderByIdRepository,
            loadServicePriceByIdRepository,
            finalizeServiceOrderRepository);

    UUID serviceOrderId = UUID.randomUUID();
    UUID washerId = UUID.randomUUID();
    params = new FinalizeServiceOrder.Params(serviceOrderId, PaymentMethod.PIX);
    Instant now = Instant.parse("2026-09-20T10:00:00Z");
    readyServiceOrder =
        new ServiceOrder(
            serviceOrderId,
            UUID.randomUUID(),
            1,
            washerId,
            ServiceOrderStatus.READY,
            now,
            now,
            now);
    servicePrice =
        new ServicePrice(1, "Lavagem Completa", new BigDecimal("60.00"), new BigDecimal("20.00"));
  }

  @Test
  void recordsPaymentAndCreditsCommissionWhenServiceOrderIsReady() throws Exception {
    when(loadServiceOrderByIdRepository.loadById(params.serviceOrderId()))
        .thenReturn(Optional.of(readyServiceOrder));
    when(loadServicePriceByIdRepository.loadById(readyServiceOrder.servicePriceId()))
        .thenReturn(Optional.of(servicePrice));
    ServiceOrder doneServiceOrder =
        new ServiceOrder(
            readyServiceOrder.id(),
            readyServiceOrder.vehicleId(),
            readyServiceOrder.servicePriceId(),
            readyServiceOrder.washerId(),
            ServiceOrderStatus.DONE,
            readyServiceOrder.scheduledAt(),
            readyServiceOrder.createdAt(),
            Instant.now());
    when(finalizeServiceOrderRepository.finalizeSale(any(), any(), any()))
        .thenReturn(doneServiceOrder);

    ServiceOrder result = sut.finalize(params);

    assertEquals(ServiceOrderStatus.DONE, result.status());
    ArgumentCaptor<WasherCommission> commissionCaptor =
        ArgumentCaptor.forClass(WasherCommission.class);
    ArgumentCaptor<ServicePayment> paymentCaptor = ArgumentCaptor.forClass(ServicePayment.class);
    verify(finalizeServiceOrderRepository)
        .finalizeSale(any(), paymentCaptor.capture(), commissionCaptor.capture());
    WasherCommission commission = commissionCaptor.getValue();
    ServicePayment payment = paymentCaptor.getValue();
    assertEquals(readyServiceOrder.id(), commission.serviceOrderId());
    assertEquals(readyServiceOrder.washerId(), commission.washerId());
    assertEquals(servicePrice.washerCommission(), commission.amount());
    assertEquals(readyServiceOrder.id(), payment.serviceOrderId());
    assertEquals(PaymentMethod.PIX, payment.method());
    assertEquals(servicePrice.price(), payment.amount());
  }

  @Test
  void throwsServiceOrderNotFoundWhenServiceOrderDoesNotExist() {
    when(loadServiceOrderByIdRepository.loadById(params.serviceOrderId()))
        .thenReturn(Optional.empty());

    assertThrows(ServiceOrderNotFoundError.class, () -> sut.finalize(params));
    verifyNoInteractions(loadServicePriceByIdRepository, finalizeServiceOrderRepository);
  }

  @Test
  void throwsInvalidServiceOrderStatusWhenNotReady() {
    ServiceOrder stillWashing =
        new ServiceOrder(
            readyServiceOrder.id(),
            readyServiceOrder.vehicleId(),
            readyServiceOrder.servicePriceId(),
            readyServiceOrder.washerId(),
            ServiceOrderStatus.WASHING,
            readyServiceOrder.scheduledAt(),
            readyServiceOrder.createdAt(),
            readyServiceOrder.updatedAt());
    when(loadServiceOrderByIdRepository.loadById(params.serviceOrderId()))
        .thenReturn(Optional.of(stillWashing));

    assertThrows(InvalidServiceOrderStatusError.class, () -> sut.finalize(params));
    verifyNoInteractions(loadServicePriceByIdRepository, finalizeServiceOrderRepository);
  }

  @Test
  void throwsServicePriceNotFoundWhenServicePriceNoLongerExists() {
    when(loadServiceOrderByIdRepository.loadById(params.serviceOrderId()))
        .thenReturn(Optional.of(readyServiceOrder));
    when(loadServicePriceByIdRepository.loadById(readyServiceOrder.servicePriceId()))
        .thenReturn(Optional.empty());

    assertThrows(ServicePriceNotFoundError.class, () -> sut.finalize(params));
    verify(finalizeServiceOrderRepository, never()).finalizeSale(any(), any(), any());
  }
}
