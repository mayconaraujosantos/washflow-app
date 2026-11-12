package com.washflow.data.usecases.sale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.db.sale.SaveSaleRepository;
import com.washflow.data.protocols.db.shared.LoadServicePriceByIdRepository;
import com.washflow.data.protocols.db.vehicle.LoadVehicleByIdRepository;
import com.washflow.domain.entities.pricing.ServicePrice;
import com.washflow.domain.entities.sale.Sale;
import com.washflow.domain.entities.sale.SaleType;
import com.washflow.domain.entities.shared.PaymentMethod;
import com.washflow.domain.errors.pricing.ServicePriceNotFoundError;
import com.washflow.domain.errors.vehicle.VehicleNotFoundError;
import com.washflow.domain.usecases.sale.CreateSale;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DbCreateSaleTest {

  private static final Instant NOW = Instant.parse("2026-11-03T14:00:00Z");
  private static final UUID MANAGER = UUID.randomUUID();

  @Mock private LoadServicePriceByIdRepository prices;
  @Mock private LoadVehicleByIdRepository vehicles;
  @Mock private SaveSaleRepository saveSaleRepository;

  private DbCreateSale sut;

  @BeforeEach
  void setUp() {
    sut = new DbCreateSale(prices, vehicles, saveSaleRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    when(prices.loadById(1))
        .thenReturn(
            Optional.of(new ServicePrice(1, "Lavagem", new BigDecimal("35.00"), BigDecimal.TEN)));
    when(prices.loadById(2))
        .thenReturn(
            Optional.of(new ServicePrice(2, "Cera", new BigDecimal("12.50"), BigDecimal.ZERO)));
    when(saveSaleRepository.save(any())).thenAnswer(call -> call.getArgument(0));
  }

  @Test
  void computesTotalsFromTheCatalogAndTracksOutstandingAmount() throws Exception {
    Sale sale =
        sut.create(
            params(
                SaleType.QUICK_SALE,
                List.of(new CreateSale.Item(1, 2), new CreateSale.Item(2, 1)),
                new BigDecimal("2.50"),
                List.of(
                    new CreateSale.Payment(PaymentMethod.PIX, new BigDecimal("50")),
                    new CreateSale.Payment(PaymentMethod.CASH, new BigDecimal("10.00")))));

    assertEquals(new BigDecimal("82.50"), sale.subtotal());
    assertEquals(new BigDecimal("80.00"), sale.total());
    assertEquals(new BigDecimal("60.00"), sale.paidAmount());
    assertEquals(new BigDecimal("20.00"), sale.outstandingAmount());
    assertEquals("Lavagem", sale.items().get(0).description());
    assertEquals(new BigDecimal("70.00"), sale.items().get(0).total());
    assertEquals(NOW, sale.createdAt());
    assertEquals(MANAGER, sale.createdBy());
  }

  @Test
  void budgetWithoutPaymentsKeepsTheWholeTotalOutstanding() throws Exception {
    Sale sale = sut.create(params(SaleType.BUDGET, List.of(new CreateSale.Item(1, 1)), null, null));

    assertEquals(SaleType.BUDGET, sale.type());
    assertEquals(BigDecimal.ZERO.setScale(2), sale.discount());
    assertEquals(new BigDecimal("35.00"), sale.outstandingAmount());
  }

  @Test
  void rejectsPaymentsAboveTheTotal() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            sut.create(
                params(
                    SaleType.QUICK_SALE,
                    List.of(new CreateSale.Item(1, 1)),
                    null,
                    List.of(new CreateSale.Payment(PaymentMethod.PIX, new BigDecimal("35.01"))))));
  }

  @Test
  void rejectsDiscountAboveSubtotalOrNegative() {
    List<CreateSale.Item> items = List.of(new CreateSale.Item(1, 1));
    assertThrows(
        IllegalArgumentException.class,
        () -> sut.create(params(SaleType.QUICK_SALE, items, new BigDecimal("36"), null)));
    assertThrows(
        IllegalArgumentException.class,
        () -> sut.create(params(SaleType.QUICK_SALE, items, new BigDecimal("-1"), null)));
  }

  @Test
  void rejectsEmptyItemsBadQuantitiesAndSubCentAmounts() {
    assertThrows(
        IllegalArgumentException.class,
        () -> sut.create(params(SaleType.QUICK_SALE, List.of(), null, null)));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            sut.create(
                params(SaleType.QUICK_SALE, List.of(new CreateSale.Item(1, 0)), null, null)));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            sut.create(
                params(
                    SaleType.QUICK_SALE,
                    List.of(new CreateSale.Item(1, 1)),
                    new BigDecimal("0.001"),
                    null)));
  }

  @Test
  void rejectsUnknownCatalogItemAndVehicle() {
    when(prices.loadById(99)).thenReturn(Optional.empty());
    assertThrows(
        ServicePriceNotFoundError.class,
        () -> sut.create(params(SaleType.BUDGET, List.of(new CreateSale.Item(99, 1)), null, null)));

    UUID vehicleId = UUID.randomUUID();
    when(vehicles.loadById(vehicleId)).thenReturn(Optional.empty());
    assertThrows(
        VehicleNotFoundError.class,
        () ->
            sut.create(
                new CreateSale.Params(
                    SaleType.BUDGET,
                    vehicleId,
                    null,
                    null,
                    List.of(new CreateSale.Item(1, 1)),
                    null,
                    null,
                    null,
                    MANAGER)));
  }

  @Test
  void rejectsMissingTypeBeforeTouchingTheDatabase() {
    assertThrows(
        IllegalArgumentException.class,
        () -> sut.create(params(null, List.of(new CreateSale.Item(1, 1)), null, null)));
    verifyNoInteractions(saveSaleRepository);
  }

  private static CreateSale.Params params(
      SaleType type,
      List<CreateSale.Item> items,
      BigDecimal discount,
      List<CreateSale.Payment> payments) {
    return new CreateSale.Params(
        type, null, "Maria", "92991234567", items, discount, payments, "obs", MANAGER);
  }
}
