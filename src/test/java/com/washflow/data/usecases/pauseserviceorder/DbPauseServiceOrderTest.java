package com.washflow.data.usecases.pauseserviceorder;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.db.PauseServiceOrderRepository;
import com.washflow.domain.errors.ServiceOrderNotPausedError;
import com.washflow.domain.usecases.PauseServiceOrder;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DbPauseServiceOrderTest {

  @Mock private PauseServiceOrderRepository pauseRepository;

  private DbPauseServiceOrder sut;
  private UUID orderId;
  private UUID washerId;

  @BeforeEach
  void setUp() {
    sut = new DbPauseServiceOrder(pauseRepository);
    orderId = UUID.randomUUID();
    washerId = UUID.randomUUID();
  }

  @Test
  void pausesSuccessfullyWhenReasonIsValid() throws Exception {
    when(pauseRepository.pause(any(), any(), any(), any())).thenReturn(true);

    PauseServiceOrder.Params params =
        new PauseServiceOrder.Params(orderId, washerId, "LUNCH", null);

    assertDoesNotThrow(() -> sut.pause(params));
    verify(pauseRepository).pause(orderId, washerId, "LUNCH", null);
  }

  @Test
  void pausesSuccessfullyWithOtherReasonAndDescription() throws Exception {
    when(pauseRepository.pause(any(), any(), any(), any())).thenReturn(true);

    PauseServiceOrder.Params params =
        new PauseServiceOrder.Params(orderId, washerId, "OTHER", "Machine breakdown");

    assertDoesNotThrow(() -> sut.pause(params));
    verify(pauseRepository).pause(eq(orderId), eq(washerId), eq("OTHER"), eq("Machine breakdown"));
  }

  @Test
  void throwsWhenReasonIsInvalid() {
    PauseServiceOrder.Params params =
        new PauseServiceOrder.Params(orderId, washerId, "INVALID", null);

    assertThrows(IllegalArgumentException.class, () -> sut.pause(params));
    verifyNoInteractions(pauseRepository);
  }

  @Test
  void throwsWhenOtherReasonHasNoDescription() {
    PauseServiceOrder.Params params =
        new PauseServiceOrder.Params(orderId, washerId, "OTHER", null);

    assertThrows(IllegalArgumentException.class, () -> sut.pause(params));
    verifyNoInteractions(pauseRepository);
  }

  @Test
  void throwsWhenDescriptionExceeds200Chars() {
    String longDesc = "a".repeat(201);
    PauseServiceOrder.Params params =
        new PauseServiceOrder.Params(orderId, washerId, "OTHER", longDesc);

    assertThrows(IllegalArgumentException.class, () -> sut.pause(params));
    verifyNoInteractions(pauseRepository);
  }

  @Test
  void throwsServiceOrderNotPausedWhenRepositoryReturnsFalse() throws Exception {
    when(pauseRepository.pause(any(), any(), any(), any())).thenReturn(false);

    PauseServiceOrder.Params params =
        new PauseServiceOrder.Params(orderId, washerId, "LUNCH", null);

    assertThrows(ServiceOrderNotPausedError.class, () -> sut.pause(params));
  }

  @Test
  void trimsDescriptionAndNullifiesBlank() throws Exception {
    when(pauseRepository.pause(any(), any(), any(), any())).thenReturn(true);

    PauseServiceOrder.Params params =
        new PauseServiceOrder.Params(orderId, washerId, "LUNCH", "   ");

    assertDoesNotThrow(() -> sut.pause(params));
    verify(pauseRepository).pause(eq(orderId), eq(washerId), eq("LUNCH"), eq(null));
  }
}
