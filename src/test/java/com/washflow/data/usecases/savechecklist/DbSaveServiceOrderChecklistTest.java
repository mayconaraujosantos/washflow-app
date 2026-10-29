package com.washflow.data.usecases.savechecklist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.db.SaveServiceOrderChecklistRepository;
import com.washflow.domain.entities.ServiceOrderChecklist;
import com.washflow.domain.entities.ServiceOrderChecklist.ChecklistItem;
import com.washflow.domain.entities.ServiceOrderChecklist.ChecklistType;
import com.washflow.domain.errors.InvalidChecklistTypeError;
import com.washflow.domain.usecases.SaveServiceOrderChecklist;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DbSaveServiceOrderChecklistTest {

  @Mock private SaveServiceOrderChecklistRepository saveRepository;

  private DbSaveServiceOrderChecklist sut;
  private UUID serviceOrderId;
  private UUID createdBy;
  private List<ChecklistItem> items;

  @BeforeEach
  void setUp() {
    sut = new DbSaveServiceOrderChecklist(saveRepository);
    serviceOrderId = UUID.randomUUID();
    createdBy = UUID.randomUUID();
    items =
        List.of(
            new ChecklistItem("Faróis", true, null),
            new ChecklistItem("Retrovisores", false, "Verificar"));
  }

  @Test
  void savesChecklistWhenParamsAreValid() throws Exception {
    SaveServiceOrderChecklist.Params params =
        new SaveServiceOrderChecklist.Params(
            serviceOrderId, ChecklistType.ENTRY, items, "base64image", createdBy);

    ServiceOrderChecklist saved =
        new ServiceOrderChecklist(
            UUID.randomUUID(),
            serviceOrderId,
            ChecklistType.ENTRY,
            items,
            "base64image",
            Instant.now(),
            createdBy);
    when(saveRepository.save(any(ServiceOrderChecklist.class))).thenReturn(saved);

    ServiceOrderChecklist result = sut.save(params);

    assertNotNull(result);
    assertEquals(ChecklistType.ENTRY, result.type());
    assertEquals(2, result.items().size());
    verify(saveRepository).save(any(ServiceOrderChecklist.class));
  }

  @Test
  void throwsInvalidChecklistTypeWhenTypeIsNull() {
    SaveServiceOrderChecklist.Params params =
        new SaveServiceOrderChecklist.Params(serviceOrderId, null, items, "base64image", createdBy);

    assertThrows(InvalidChecklistTypeError.class, () -> sut.save(params));
    verifyNoInteractions(saveRepository);
  }

  @Test
  void throwsInvalidChecklistTypeWhenItemsAreEmpty() {
    SaveServiceOrderChecklist.Params params =
        new SaveServiceOrderChecklist.Params(
            serviceOrderId, ChecklistType.ENTRY, List.of(), "base64image", createdBy);

    assertThrows(InvalidChecklistTypeError.class, () -> sut.save(params));
    verifyNoInteractions(saveRepository);
  }

  @Test
  void savesExitChecklist() throws Exception {
    SaveServiceOrderChecklist.Params params =
        new SaveServiceOrderChecklist.Params(
            serviceOrderId, ChecklistType.EXIT, items, null, createdBy);

    ServiceOrderChecklist saved =
        new ServiceOrderChecklist(
            UUID.randomUUID(),
            serviceOrderId,
            ChecklistType.EXIT,
            items,
            null,
            Instant.now(),
            createdBy);
    when(saveRepository.save(any(ServiceOrderChecklist.class))).thenReturn(saved);

    ServiceOrderChecklist result = sut.save(params);

    assertEquals(ChecklistType.EXIT, result.type());
    assertNotNull(result);
  }
}
