package com.washflow.infra.db.jdbi.whatsapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.washflow.data.tenancy.TenantContext;
import com.washflow.domain.entities.whatsapp.Conversation;
import com.washflow.domain.entities.whatsapp.ConversationDraft;
import com.washflow.domain.entities.whatsapp.ConversationStep;
import com.washflow.infra.db.jdbi.TestDatabase;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConversationJdbiRepositoryIntegrationTest {

  private Jdbi jdbi;
  private ConversationJdbiRepository repository;
  private final Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);

  @BeforeEach
  void setUp() {
    jdbi = TestDatabase.prepare();
    repository = new ConversationJdbiRepository(jdbi);
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void keepsTheStepAndTheDraftOfEachPhone() {
    ConversationDraft draft = new ConversationDraft();
    UUID vehicle = UUID.randomUUID();
    draft.setVehicleId(vehicle);
    draft.setServiceIds(List.of(1, 2));
    draft.setDay(LocalDate.parse("2026-11-21"));
    draft.setTime(LocalTime.of(14, 0));
    draft.setSlotOptions(List.of(Instant.parse("2026-11-21T18:00:00Z")));

    repository.save("5592991234567", new Conversation(ConversationStep.CHOOSE_TIME, draft, now));
    repository.save(
        "5592991234567", new Conversation(ConversationStep.CHOOSE_TIME, draft, now.plusSeconds(5)));

    Conversation loaded = repository.load("5592991234567").orElseThrow();
    assertEquals(ConversationStep.CHOOSE_TIME, loaded.step());
    assertEquals(now.plusSeconds(5), loaded.updatedAt());
    assertEquals(vehicle, loaded.draft().getVehicleId());
    assertEquals(List.of(1, 2), loaded.draft().getServiceIds());
    assertEquals(LocalDate.parse("2026-11-21"), loaded.draft().getDay());
    assertEquals(LocalTime.of(14, 0), loaded.draft().getTime());
    assertEquals(List.of(Instant.parse("2026-11-21T18:00:00Z")), loaded.draft().getSlotOptions());
    assertTrue(repository.load("5592000000000").isEmpty());
  }

  @Test
  void aMessageIdIsReceivedOnceAndForgottenLater() {
    assertTrue(repository.markReceived("ABC", now));
    assertFalse(repository.markReceived("ABC", now));

    assertEquals(1, repository.forgetReceivedBefore(now.plus(Duration.ofSeconds(1))));
    assertTrue(repository.markReceived("ABC", now));
  }

  @Test
  void knowsTheLavaJatoWhileItIsActive() {
    assertEquals(Optional.of("Lava Teste"), repository.activeEstablishmentName());

    jdbi.useHandle(
        handle ->
            handle.execute(
                "UPDATE estabelecimentos SET ativo = FALSE WHERE id = ?",
                TestDatabase.ESTABLISHMENT_ID));

    assertTrue(repository.activeEstablishmentName().isEmpty());
  }
}
