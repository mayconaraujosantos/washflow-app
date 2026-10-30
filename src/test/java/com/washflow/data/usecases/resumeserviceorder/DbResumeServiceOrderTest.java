package com.washflow.data.usecases.resumeserviceorder;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.db.ResumeServiceOrderRepository;
import com.washflow.domain.errors.ServiceOrderNotWashingError;
import com.washflow.domain.usecases.ResumeServiceOrder;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DbResumeServiceOrderTest {

  @Mock private ResumeServiceOrderRepository resumeRepository;

  private DbResumeServiceOrder sut;
  private UUID orderId;
  private UUID washerId;

  @BeforeEach
  void setUp() {
    sut = new DbResumeServiceOrder(resumeRepository);
    orderId = UUID.randomUUID();
    washerId = UUID.randomUUID();
  }

  @Test
  void resumesSuccessfully() throws Exception {
    when(resumeRepository.resume(any(), any())).thenReturn(true);

    ResumeServiceOrder.Params params = new ResumeServiceOrder.Params(orderId, washerId);

    assertDoesNotThrow(() -> sut.resume(params));
    verify(resumeRepository).resume(orderId, washerId);
  }

  @Test
  void throwsServiceOrderNotWashingWhenRepositoryReturnsFalse() throws Exception {
    when(resumeRepository.resume(any(), any())).thenReturn(false);

    ResumeServiceOrder.Params params = new ResumeServiceOrder.Params(orderId, washerId);

    assertThrows(ServiceOrderNotWashingError.class, () -> sut.resume(params));
    verify(resumeRepository).resume(orderId, washerId);
  }
}
