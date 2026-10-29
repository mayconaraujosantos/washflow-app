package com.washflow.infra.db.jdbi;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.washflow.data.protocols.db.LoadServiceOrderChecklistRepository;
import com.washflow.data.protocols.db.SaveServiceOrderChecklistRepository;
import com.washflow.domain.entities.ServiceOrderChecklist;
import com.washflow.domain.entities.ServiceOrderChecklist.ChecklistItem;
import com.washflow.domain.entities.ServiceOrderChecklist.ChecklistType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.StatementContext;

public class ChecklistJdbiRepository
    implements SaveServiceOrderChecklistRepository, LoadServiceOrderChecklistRepository {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final String INSERT_SQL =
      """
      INSERT INTO service_order_checklist
        (id, atendimento_id, tipo, itens, assinatura_imagem, criado_em, criado_por)
      VALUES
        (:id, :serviceOrderId, :type, :items, :signatureImage, :createdAt, :createdBy)
      ON CONFLICT (atendimento_id, tipo)
      DO UPDATE SET
        itens = EXCLUDED.itens,
        assinatura_imagem = EXCLUDED.assinatura_imagem,
        criado_em = EXCLUDED.criado_em,
        criado_por = EXCLUDED.createdBy
      """;

  private static final String SELECT_BY_ORDER_SQL =
      """
      SELECT id, atendimento_id, tipo, itens, assinatura_imagem, criado_em, criado_por
      FROM service_order_checklist
      WHERE atendimento_id = :serviceOrderId
      ORDER BY criado_em ASC
      """;

  private static final String SELECT_BY_ORDER_AND_TYPE_SQL =
      """
      SELECT id, atendimento_id, tipo, itens, assinatura_imagem, criado_em, criado_por
      FROM service_order_checklist
      WHERE atendimento_id = :serviceOrderId AND tipo = :type
      """;

  private final Jdbi jdbi;

  public ChecklistJdbiRepository(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  private ServiceOrderChecklist mapRow(ResultSet rs, StatementContext ctx) throws SQLException {
    UUID id = UUID.fromString(rs.getString("id"));
    UUID serviceOrderId = UUID.fromString(rs.getString("atendimento_id"));
    ChecklistType type = ChecklistType.valueOf(rs.getString("tipo"));
    List<ChecklistItem> items = parseItems(rs.getString("itens"));
    String signatureImage = rs.getString("assinatura_imagem");
    Instant createdAt = rs.getTimestamp("criado_em").toInstant();
    UUID createdBy = rs.getObject("criado_por", UUID.class);
    return new ServiceOrderChecklist(
        id, serviceOrderId, type, items, signatureImage, createdAt, createdBy);
  }

  private List<ChecklistItem> parseItems(String json) throws SQLException {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      return MAPPER.readValue(json, new TypeReference<List<ChecklistItem>>() {});
    } catch (Exception e) {
      throw new SQLException("Failed to parse checklist items JSON", e);
    }
  }

  private String serializeItems(List<ChecklistItem> items) {
    try {
      return MAPPER.writeValueAsString(items);
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize checklist items", e);
    }
  }

  @Override
  public ServiceOrderChecklist save(ServiceOrderChecklist checklist) {
    jdbi.useHandle(
        handle -> {
          handle
              .createUpdate(INSERT_SQL)
              .bind("id", checklist.id())
              .bind("serviceOrderId", checklist.serviceOrderId())
              .bind("type", checklist.type().name())
              .bind("items", serializeItems(checklist.items()))
              .bind("signatureImage", checklist.signatureImage())
              .bind("createdAt", checklist.createdAt())
              .bind("createdBy", checklist.createdBy())
              .execute();
        });
    return checklist;
  }

  @Override
  public List<ServiceOrderChecklist> loadByServiceOrderId(UUID serviceOrderId) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(SELECT_BY_ORDER_SQL)
                .bind("serviceOrderId", serviceOrderId)
                .map(this::mapRow)
                .list());
  }

  @Override
  public Optional<ServiceOrderChecklist> loadByServiceOrderIdAndType(
      UUID serviceOrderId, ChecklistType type) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(SELECT_BY_ORDER_AND_TYPE_SQL)
                .bind("serviceOrderId", serviceOrderId)
                .bind("type", type.name())
                .map(this::mapRow)
                .findFirst());
  }
}
