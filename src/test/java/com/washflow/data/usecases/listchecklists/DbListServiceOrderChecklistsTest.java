package com.washflow.data.usecases.listchecklists;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.db.LoadServiceOrderChecklistRepository;
import com.washflow.domain.entities.ServiceOrderChecklist;
import com.washflow.domain.entities.ServiceOrderChecklist.ChecklistItem;
import com.washflow.domain.entities.ServiceOrderChecklist.ChecklistType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DbListServiceOrderChecklistsTest {

  @Mock private LoadServiceOrderChecklistRepository loadRepository;

  private DbListServiceOrderChecklists sut;
  private UUID serviceOrderId;

  @BeforeEach
  void setUp() {
    sut = new DbListServiceOrderChecklists(loadRepository);
    serviceOrderId = UUID.randomUUID();
  }

  @Test
  void returnsChecklistsWhenExist() {
    List<ChecklistItem> items = List.of(new ChecklistItem("Faróis", true, null));
    ServiceOrderChecklist checklist =
        new ServiceOrderChecklist(
            UUID.randomUUID(),
            serviceOrderId,
            ChecklistType.ENTRY,
            items,
            null,
            Instant.now(),
            UUID.randomUUID());
    when(loadRepository.loadByServiceOrderId(serviceOrderId)).thenReturn(List.of(checklist));

    List<ServiceOrderChecklist> result = sut.list(serviceOrderId);

    assertEquals(1, result.size());
    assertEquals(ChecklistType.ENTRY, result.get(0).type());
    verify(loadRepository).loadByServiceOrderId(serviceOrderId);
  }

  @Test
  void returnsEmptyListWhenNoChecklists() {
    when(loadRepository.loadByServiceOrderId(serviceOrderId)).thenReturn(List.of());

    List<ServiceOrderChecklist> result = sut.list(serviceOrderId);

    assertEquals(0, result.size());
    verify(loadRepository).loadByServiceOrderId(serviceOrderId);
  }
}
