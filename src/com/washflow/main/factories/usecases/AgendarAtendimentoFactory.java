package com.washflow.main.factories.usecases;

import com.washflow.data.usecases.agendaratendimento.DbAgendarAtendimento;
import com.washflow.domain.usecases.AgendarAtendimento;
import com.washflow.infra.db.jdbi.AtendimentoJdbiRepository;
import com.washflow.infra.db.jdbi.ServicoPrecoJdbiRepository;
import com.washflow.infra.db.jdbi.VeiculoJdbiRepository;
import org.jdbi.v3.core.Jdbi;

/**
 * Swapping JDBI for Hibernate later means changing only the three {@code new *JdbiRepository(...)}
 * calls here - {@code DbAgendarAtendimento} depends on the {@code data.protocols.db} interfaces,
 * not on these concrete classes.
 */
public final class AgendarAtendimentoFactory {

  private AgendarAtendimentoFactory() {}

  public static AgendarAtendimento make(Jdbi jdbi) {
    return new DbAgendarAtendimento(
        new VeiculoJdbiRepository(jdbi),
        new ServicoPrecoJdbiRepository(jdbi),
        new AtendimentoJdbiRepository(jdbi));
  }
}
