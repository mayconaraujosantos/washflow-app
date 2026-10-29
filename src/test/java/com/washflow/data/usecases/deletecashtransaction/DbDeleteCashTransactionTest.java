package com.washflow.data.usecases.deletecashtransaction;

import static org.mockito.Mockito.verify;

import com.washflow.data.protocols.db.DeleteCashTransactionRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DbDeleteCashTransactionTest {

  @Mock private DeleteCashTransactionRepository deleteRepository;

  private DbDeleteCashTransaction sut;

  @BeforeEach
  void setUp() {
    sut = new DbDeleteCashTransaction(deleteRepository);
  }

  @Test
  void deletesTransaction() {
    UUID id = UUID.randomUUID();
    sut.delete(id);
    verify(deleteRepository).deleteById(id);
  }
}
