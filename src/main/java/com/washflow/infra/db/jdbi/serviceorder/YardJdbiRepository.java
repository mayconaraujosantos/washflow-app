package com.washflow.infra.db.jdbi.serviceorder;

import com.washflow.data.protocols.db.yard.LoadWaitingQueueRepository;
import com.washflow.data.protocols.db.yard.LoadWasherBoardRepository;
import com.washflow.data.protocols.db.yard.LoadWashingByWasherRepository;
import com.washflow.data.protocols.db.yard.LoadYardOverviewRepository;
import com.washflow.domain.entities.serviceorder.ServiceOrder;
import com.washflow.domain.usecases.yard.GetWasherBoard;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jdbi.v3.core.Jdbi;

/** The yard screens' reads: waiting queue, yard overview and a washer's active washes. */
public class YardJdbiRepository
    implements LoadWaitingQueueRepository,
        LoadYardOverviewRepository,
        LoadWashingByWasherRepository,
        LoadWasherBoardRepository {

  // One row per active washer: the wash in progress or paused (if any), its open pause, and the
  // waiting cars the manager reserved for them.
  private static final String WASHER_BOARD_SQL =
      """
      SELECT u.id, u.nome, u.telefone,
             cur.id AS order_id, cur.status, cur.placa, cur.modelo, cur.lavagem_iniciada_em,
             pz.motivo AS pause_reason, pz.iniciada_em AS paused_at,
             (SELECT COUNT(*) FROM atendimentos r
              WHERE r.lavador_id = u.id AND r.status = 'AGUARDANDO_PATIO'
                AND r.estabelecimento_id = :tenant) AS reserved
      FROM usuarios u
      LEFT JOIN LATERAL (
        SELECT a.id, a.status, v.placa, v.modelo, a.lavagem_iniciada_em
        FROM atendimentos a JOIN veiculos v ON v.id = a.veiculo_id
        WHERE a.lavador_id = u.id AND a.status IN ('EM_LAVAGEM', 'PAUSADA')
          AND a.estabelecimento_id = :tenant
        ORDER BY a.atualizado_em DESC
        LIMIT 1
      ) cur ON TRUE
      LEFT JOIN LATERAL (
        SELECT p.motivo, p.iniciada_em FROM pausas_atendimento p
        WHERE p.atendimento_id = cur.id AND p.retomada_em IS NULL
        ORDER BY p.iniciada_em DESC
        LIMIT 1
      ) pz ON cur.status = 'PAUSADA'
      WHERE u.estabelecimento_id = :tenant AND u.perfil = 'LAVADOR' AND u.ativo
      """;

  private static final String WAITING_QUEUE_SQL =
      ServiceOrderRows.SELECT_WITH_DETAILS
          + """
      WHERE a.status = 'AGUARDANDO_PATIO' AND a.estabelecimento_id = :tenant
        AND (CAST(:washerId AS UUID) IS NULL OR a.lavador_id IS NULL
             OR a.lavador_id = CAST(:washerId AS UUID))
      ORDER BY (a.lavador_id IS NULL), a.atualizado_em ASC
      """;

  private static final String YARD_OVERVIEW_SQL =
      ServiceOrderRows.SELECT_WITH_DETAILS
          + """
      WHERE a.status NOT IN ('FINALIZADO', 'CANCELADO') AND a.estabelecimento_id = :tenant
      ORDER BY
        CASE a.status
          WHEN 'EM_LAVAGEM' THEN 0
          WHEN 'PAUSADA' THEN 0
          WHEN 'AGUARDANDO_PATIO' THEN 1
          WHEN 'PRONTO' THEN 2
          WHEN 'AGENDADO' THEN 3
          ELSE 4
        END,
        a.atualizado_em ASC
      """;

  private static final String WASHER_ACTIVE_SQL =
      ServiceOrderRows.SELECT_WITH_DETAILS
          + """
      WHERE a.status IN ('EM_LAVAGEM', 'PAUSADA') AND a.lavador_id = :washerId
        AND a.estabelecimento_id = :tenant
      ORDER BY a.atualizado_em ASC
      """;

  private final Jdbi jdbi;

  public YardJdbiRepository(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  @Override
  public List<ServiceOrder> loadWaitingQueue(UUID washerId) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(WAITING_QUEUE_SQL)
                .bind("washerId", washerId == null ? null : washerId.toString())
                .map(ServiceOrderRows::map)
                .list());
  }

  @Override
  public List<ServiceOrder> loadYardOverview() {
    return jdbi.withHandle(
        handle -> handle.createQuery(YARD_OVERVIEW_SQL).map(ServiceOrderRows::map).list());
  }

  @Override
  public List<ServiceOrder> loadByWasherId(UUID washerId) {
    return loadWashingByWasher(washerId);
  }

  public List<ServiceOrder> loadWashingByWasher(UUID washerId) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(WASHER_ACTIVE_SQL)
                .bind("washerId", washerId)
                .map(ServiceOrderRows::map)
                .list());
  }

  @Override
  public List<GetWasherBoard.WasherStatus> loadWasherBoard() {
    return jdbi.withHandle(
        handle -> handle.createQuery(WASHER_BOARD_SQL).map((rs, ctx) -> washer(rs)).list());
  }

  private static GetWasherBoard.WasherStatus washer(ResultSet rs) throws SQLException {
    UUID orderId = rs.getObject("order_id", UUID.class);
    String state;
    GetWasherBoard.CurrentWash current = null;
    if (orderId == null) {
      state = GetWasherBoard.WasherStatus.FREE;
    } else {
      boolean paused = "PAUSADA".equals(rs.getString("status"));
      state = paused ? GetWasherBoard.WasherStatus.PAUSED : GetWasherBoard.WasherStatus.WASHING;
      current =
          new GetWasherBoard.CurrentWash(
              orderId,
              rs.getString("placa"),
              rs.getString("modelo"),
              instant(rs.getTimestamp("lavagem_iniciada_em")),
              paused ? rs.getString("pause_reason") : null,
              paused ? instant(rs.getTimestamp("paused_at")) : null);
    }
    return new GetWasherBoard.WasherStatus(
        rs.getObject("id", UUID.class),
        rs.getString("nome"),
        rs.getString("telefone"),
        state,
        current,
        rs.getInt("reserved"));
  }

  private static Instant instant(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toInstant();
  }
}
