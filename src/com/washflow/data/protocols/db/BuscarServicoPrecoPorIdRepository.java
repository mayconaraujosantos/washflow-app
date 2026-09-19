package com.washflow.data.protocols.db;

import com.washflow.domain.entities.ServicoPreco;
import java.util.Optional;

public interface BuscarServicoPrecoPorIdRepository {

  Optional<ServicoPreco> buscarPorId(int id);
}
