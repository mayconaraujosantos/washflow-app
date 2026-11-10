package com.washflow.data.usecases.sale;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.db.sale.LoadSalesRepository;
import com.washflow.domain.entities.SaleRefund;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DbListSalesTest {

  @Mock private LoadSalesRepository repository;
  @Mock private SaleRefund refund;

  @Test
  void listsRefundsMadeBetweenTwoManausDates() {
    List<SaleRefund> refunds = List.of(refund);
    when(repository.loadRefundsBetween(
            Instant.parse("2026-11-01T04:00:00Z"), Instant.parse("2026-11-11T04:00:00Z")))
        .thenReturn(refunds);

    assertSame(
        refunds,
        new DbListSales(repository)
            .refunds(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-10")));
  }

  @Test
  void refusesInvertedOrTooLongRanges() {
    DbListSales sut = new DbListSales(repository);

    assertThrows(
        IllegalArgumentException.class,
        () -> sut.refunds(LocalDate.parse("2026-11-10"), LocalDate.parse("2026-11-01")));
    assertThrows(
        IllegalArgumentException.class,
        () -> sut.refunds(LocalDate.parse("2025-01-01"), LocalDate.parse("2026-11-01")));
    verifyNoInteractions(repository);
  }
}
