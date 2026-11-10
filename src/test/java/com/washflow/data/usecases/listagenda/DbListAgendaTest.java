package com.washflow.data.usecases.listagenda;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

import com.washflow.data.protocols.db.LoadServiceOrdersScheduledBetweenRepository;
import com.washflow.domain.usecases.schedule.ListAgenda;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DbListAgendaTest {

  @Mock private LoadServiceOrdersScheduledBetweenRepository repository;

  @Test
  void convertsShopLocalDatesToAnInclusiveInstantRange() {
    new DbListAgenda(repository)
        .list(new ListAgenda.Params(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)));

    // Manaus is UTC-4: 00:00 local on Nov 1st up to 00:00 local on Dec 1st.
    verify(repository)
        .loadScheduledBetween(
            Instant.parse("2026-11-01T04:00:00Z"), Instant.parse("2026-12-01T04:00:00Z"));
  }

  @Test
  void rejectsReversedOrTooLongRanges() {
    DbListAgenda sut = new DbListAgenda(repository);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            sut.list(new ListAgenda.Params(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 1))));
    assertThrows(
        IllegalArgumentException.class,
        () -> sut.list(new ListAgenda.Params(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 5))));
  }
}
