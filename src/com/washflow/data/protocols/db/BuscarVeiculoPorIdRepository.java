package com.washflow.data.protocols.db;

import com.washflow.domain.entities.Veiculo;
import java.util.Optional;
import java.util.UUID;

public interface BuscarVeiculoPorIdRepository {

  Optional<Veiculo> buscarPorId(UUID id);
}
