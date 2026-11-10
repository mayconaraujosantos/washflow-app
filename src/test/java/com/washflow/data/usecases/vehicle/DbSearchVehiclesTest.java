package com.washflow.data.usecases.vehicle;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.washflow.data.protocols.db.vehicle.SearchVehiclesRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DbSearchVehiclesTest {

  @Mock private SearchVehiclesRepository repository;

  @Test
  void ignoresQueriesShorterThanTwoCharacters() {
    DbSearchVehicles sut = new DbSearchVehicles(repository);

    assertTrue(sut.search(" a ").isEmpty());
    assertTrue(sut.search(null).isEmpty());
    verifyNoInteractions(repository);
  }

  @Test
  void normalizesPlateNameAndPhoneFragments() {
    new DbSearchVehicles(repository).search(" abc-1d ");

    verify(repository).search("ABC1D", "abc-1d", "", 10);
  }

  @Test
  void searchesPhoneDigitsFromThreeDigitsOn() {
    new DbSearchVehicles(repository).search("(11) 98");

    verify(repository).search("1198", "(11) 98", "1198", 10);
  }
}
