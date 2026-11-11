package com.washflow.data.usecases.serviceorder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.db.serviceorder.FinalizeServiceOrderRepository;
import com.washflow.data.protocols.db.serviceorder.LoadServiceOrderByIdRepository;
import com.washflow.data.protocols.db.shared.LoadServicePriceByIdRepository;
import com.washflow.domain.entities.pricing.ServicePrice;
import com.washflow.domain.entities.serviceorder.ServiceOrder;
import com.washflow.domain.entities.serviceorder.ServiceOrderStatus;
import com.washflow.domain.entities.serviceorder.ServicePayment;
import com.washflow.domain.entities.serviceorder.WasherCommission;
import com.washflow.domain.entities.shared.PaymentMethod;
import com.washflow.domain.errors.finance.ServicePriceNotFoundError;
import com.washflow.domain.errors.serviceorder.InvalidServiceOrderStatusError;
import com.washflow.domain.errors.serviceorder.ServiceOrderNotFoundError;
import com.washflow.domain.usecases.serviceorder.FinalizeServiceOrder;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
        .thenReturn(Optional.of(doneServiceOrder));

    ServiceOrder result = sut.finalizeOrder(params);

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
  void sumsSelectedServicePricesAndFixedCommissions() throws Exception {
    ServiceOrder combinedOrder =
        new ServiceOrder(
            readyServiceOrder.id(),
            readyServiceOrder.vehicleId(),
            List.of(1, 2),
            readyServiceOrder.washerId(),
            ServiceOrderStatus.READY,
            readyServiceOrder.scheduledAt(),
            readyServiceOrder.createdAt(),
            readyServiceOrder.updatedAt());
    ServicePrice polishing =
        new ServicePrice(2, "Polimento", new BigDecimal("45.00"), new BigDecimal("5.00"));
    when(loadServiceOrderByIdRepository.loadById(params.serviceOrderId()))
        .thenReturn(Optional.of(combinedOrder));
    when(loadServicePriceByIdRepository.loadById(1)).thenReturn(Optional.of(servicePrice));
    when(loadServicePriceByIdRepository.loadById(2)).thenReturn(Optional.of(polishing));
    when(finalizeServiceOrderRepository.finalizeSale(any(), any(), any()))
        .thenReturn(Optional.of(combinedOrder));

    sut.finalizeOrder(params);

    ArgumentCaptor<WasherCommission> commissionCaptor =
        ArgumentCaptor.forClass(WasherCommission.class);
    ArgumentCaptor<ServicePayment> paymentCaptor = ArgumentCaptor.forClass(ServicePayment.class);
    verify(finalizeServiceOrderRepository)
        .finalizeSale(any(), paymentCaptor.capture(), commissionCaptor.capture());
    assertEquals(new BigDecimal("105.00"), paymentCaptor.getValue().amount());
    assertEquals(new BigDecimal("25.00"), commissionCaptor.getValue().amount());
  }

  @Test
  void throwsServiceOrderNotFoundWhenServiceOrderDoesNotExist() {
    when(loadServiceOrderByIdRepository.loadById(params.serviceOrderId()))
        .thenReturn(Optional.empty());

    assertThrows(ServiceOrderNotFoundError.class, () -> sut.finalizeOrder(params));
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

    assertThrows(InvalidServiceOrderStatusError.class, () -> sut.finalizeOrder(params));
    verifyNoInteractions(loadServicePriceByIdRepository, finalizeServiceOrderRepository);
  }

  @Test
  void throwsServicePriceNotFoundWhenServicePriceNoLongerExists() {
    when(loadServiceOrderByIdRepository.loadById(params.serviceOrderId()))
        .thenReturn(Optional.of(readyServiceOrder));
    when(loadServicePriceByIdRepository.loadById(readyServiceOrder.servicePriceId()))
        .thenReturn(Optional.empty());

    assertThrows(ServicePriceNotFoundError.class, () -> sut.finalizeOrder(params));
    verify(finalizeServiceOrderRepository, never()).finalizeSale(any(), any(), any());
  }
}
