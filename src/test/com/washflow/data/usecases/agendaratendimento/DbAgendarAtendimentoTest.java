package com.washflow.data.usecases.agendaratendimento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.db.BuscarServicoPrecoPorIdRepository;
import com.washflow.data.protocols.db.BuscarVeiculoPorIdRepository;
import com.washflow.data.protocols.db.CriarAtendimentoRepository;
import com.washflow.domain.entities.Atendimento;
import com.washflow.domain.entities.ServicoPreco;
import com.washflow.domain.entities.StatusAtendimento;
import com.washflow.domain.entities.Veiculo;
import com.washflow.domain.errors.ServicoPrecoNaoEncontradoError;
import com.washflow.domain.errors.VeiculoNaoEncontradoError;
import com.washflow.domain.usecases.AgendarAtendimento;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The payoff of keeping this use case behind {@code data.protocols.db} ports: it's tested here with
 * plain Mockito doubles, no Postgres/JDBI/Testcontainers involved at all.
 */
@ExtendWith(MockitoExtension.class)
class DbAgendarAtendimentoTest {

  @Mock private BuscarVeiculoPorIdRepository buscarVeiculoPorIdRepository;
  @Mock private BuscarServicoPrecoPorIdRepository buscarServicoPrecoPorIdRepository;
  @Mock private CriarAtendimentoRepository criarAtendimentoRepository;

  private DbAgendarAtendimento sut;
  private AgendarAtendimento.Params params;
  private Veiculo veiculo;
  private ServicoPreco servicoPreco;

  @BeforeEach
  void setUp() {
    sut =
        new DbAgendarAtendimento(
            buscarVeiculoPorIdRepository,
            buscarServicoPrecoPorIdRepository,
            criarAtendimentoRepository);

    UUID veiculoId = UUID.randomUUID();
    params = new AgendarAtendimento.Params(veiculoId, 1, Instant.parse("2026-09-20T10:00:00Z"));
    veiculo = new Veiculo(veiculoId, UUID.randomUUID(), "ABC1D23", "Onix", "Prata");
    servicoPreco =
        new ServicoPreco(1, "Lavagem Completa", new BigDecimal("60.00"), new BigDecimal("20.00"));
  }

  @Test
  void agendaComStatusAgendadoQuandoVeiculoEServicoExistem() throws Exception {
    when(buscarVeiculoPorIdRepository.buscarPorId(params.veiculoId()))
        .thenReturn(Optional.of(veiculo));
    when(buscarServicoPrecoPorIdRepository.buscarPorId(params.servicoPrecoId()))
        .thenReturn(Optional.of(servicoPreco));
    when(criarAtendimentoRepository.criar(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Atendimento atendimento = sut.agendar(params);

    assertEquals(StatusAtendimento.AGENDADO, atendimento.status());
    assertEquals(params.veiculoId(), atendimento.veiculoId());
    assertEquals(params.servicoPrecoId(), atendimento.servicoPrecoId());
    assertEquals(params.dataAgendamento(), atendimento.dataAgendamento());
    verify(criarAtendimentoRepository).criar(any());
  }

  @Test
  void lancaVeiculoNaoEncontradoQuandoVeiculoNaoExiste() {
    when(buscarVeiculoPorIdRepository.buscarPorId(params.veiculoId())).thenReturn(Optional.empty());

    assertThrows(VeiculoNaoEncontradoError.class, () -> sut.agendar(params));
    verifyNoInteractions(criarAtendimentoRepository);
  }

  @Test
  void lancaServicoPrecoNaoEncontradoQuandoServicoNaoExiste() {
    when(buscarVeiculoPorIdRepository.buscarPorId(params.veiculoId()))
        .thenReturn(Optional.of(veiculo));
    when(buscarServicoPrecoPorIdRepository.buscarPorId(params.servicoPrecoId()))
        .thenReturn(Optional.empty());

    assertThrows(ServicoPrecoNaoEncontradoError.class, () -> sut.agendar(params));
    verifyNoInteractions(criarAtendimentoRepository);
  }
}
