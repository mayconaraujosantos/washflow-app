package com.washflow.infra.db.jdbi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.washflow.data.usecases.createsale.DbCreateSale;
import com.washflow.data.usecases.createserviceprice.DbCreateServicePrice;
import com.washflow.data.usecases.listsales.DbListSales;
import com.washflow.domain.entities.PaymentMethod;
import com.washflow.domain.entities.Sale;
import com.washflow.domain.entities.SaleType;
import com.washflow.domain.entities.ServicePrice;
import com.washflow.domain.usecases.pricing.CreateServicePrice;
import com.washflow.domain.usecases.sale.CreateSale;
import com.washflow.domain.usecases.sale.ListSales;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;

class SaleIntegrationTest {

  private static final Instant NOW = Instant.parse("2026-11-03T14:00:00Z");
  private static final UUID DEMO_MANAGER = UUID.fromString("44444444-4444-4444-4444-444444444444");

  @Test
  void quickSaleWithNewCatalogItemIsPersistedWithItemsAndPayments() throws Exception {
    Jdbi jdbi = createJdbi();
    ServicePrice wax =
        new DbCreateServicePrice(new ServicePriceJdbiRepository(jdbi))
            .create(
                new CreateServicePrice.Params(
                    "Cera líquida", new BigDecimal("15.5"), new BigDecimal("2")));
    assertTrue(wax.id() >= 1000, "new catalog ids must not collide with seeded ones");

    DbCreateSale createSale =
        new DbCreateSale(
            new ServicePriceJdbiRepository(jdbi),
            new VehicleJdbiRepository(jdbi),
            new SaleJdbiRepository(jdbi),
            Clock.fixed(NOW, ZoneOffset.UTC));
    Sale sale =
        createSale.create(
            new CreateSale.Params(
                SaleType.QUICK_SALE,
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                null,
                null,
                List.of(new CreateSale.Item(1, 1), new CreateSale.Item(wax.id(), 2)),
                new BigDecimal("1.00"),
                List.of(
                    new CreateSale.Payment(PaymentMethod.PIX, new BigDecimal("50.00")),
                    new CreateSale.Payment(PaymentMethod.CASH, new BigDecimal("20.00"))),
                "Cliente pediu nota",
                DEMO_MANAGER));
    createSale.create(
        new CreateSale.Params(
            SaleType.BUDGET,
            null,
            "Ana",
            null,
            List.of(new CreateSale.Item(1, 1)),
            null,
            List.of(),
            null,
            DEMO_MANAGER));

    DbListSales listSales = new DbListSales(new SaleJdbiRepository(jdbi));
    LocalDate today = LocalDate.of(2026, 11, 3);
    List<Sale> quickSales = listSales.list(new ListSales.Params(SaleType.QUICK_SALE, today, today));

    assertEquals(1, quickSales.size());
    Sale loaded = quickSales.get(0);
    assertEquals(sale.id(), loaded.id());
    assertEquals(new BigDecimal("91.00"), loaded.subtotal());
    assertEquals(new BigDecimal("90.00"), loaded.total());
    assertEquals(new BigDecimal("70.00"), loaded.paidAmount());
    assertEquals(new BigDecimal("20.00"), loaded.outstandingAmount());
    assertEquals(2, loaded.items().size());
    assertEquals(2, loaded.payments().size());
    assertEquals(2, listSales.list(new ListSales.Params(null, today, today)).size());
    assertEquals(
        0, listSales.list(new ListSales.Params(null, today.plusDays(1), today.plusDays(1))).size());
  }

  private static Jdbi createJdbi() {
    DataSource dataSource = TestDatabase.newDatabase();
    return TestDatabase.prepare(dataSource);
  }
}
