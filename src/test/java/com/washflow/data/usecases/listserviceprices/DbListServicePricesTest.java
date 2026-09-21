package com.washflow.data.usecases.listserviceprices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.db.LoadAllServicePricesRepository;
import com.washflow.domain.entities.ServicePrice;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Same payoff as {@code DbListVehiclesByCustomerTest}: exercised here with plain Mockito doubles,
 * no Postgres/JDBI/Testcontainers involved at all.
 */
@ExtendWith(MockitoExtension.class)
class DbListServicePricesTest {

  @Mock private LoadAllServicePricesRepository loadAllServicePricesRepository;

  @Test
  void returnsAllServicePrices() {
    List<ServicePrice> servicePrices =
        List.of(
            new ServicePrice(
                1, "Lavagem Simples", new BigDecimal("40.00"), new BigDecimal("15.00")),
            new ServicePrice(
                2, "Lavagem Completa", new BigDecimal("60.00"), new BigDecimal("20.00")));
    when(loadAllServicePricesRepository.loadAll()).thenReturn(servicePrices);

    DbListServicePrices sut = new DbListServicePrices(loadAllServicePricesRepository);

    assertEquals(servicePrices, sut.list());
  }
}
