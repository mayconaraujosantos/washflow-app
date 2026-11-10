package com.washflow.infra.db.jdbi.sale;

import com.washflow.data.protocols.db.sale.LoadSalesRepository;
import com.washflow.data.protocols.db.sale.RefundSaleRepository;
import com.washflow.data.protocols.db.sale.SaveSaleRepository;
import com.washflow.domain.entities.sale.Sale;
import com.washflow.domain.entities.sale.SaleItem;
import com.washflow.domain.entities.sale.SalePayment;
import com.washflow.domain.entities.sale.SaleRefund;
import com.washflow.domain.entities.sale.SaleType;
import com.washflow.domain.entities.shared.PaymentMethod;
import com.washflow.domain.errors.sale.SaleNotFoundError;
import com.washflow.domain.errors.sale.SaleRefundNotAllowedError;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.StatementContext;

public class SaleJdbiRepository
    implements SaveSaleRepository, LoadSalesRepository, RefundSaleRepository {

  private static final String TOTAL = "total";
  private static final String METHOD = "method";
  private static final String CRIADO_POR = "criado_por";
  private static final String CRIADO_EM = "criado_em";
  private static final String VENDA_ID = "venda_id";
  private static final String VALOR = "valor";
  private static final String DESCRICAO = "descricao";
  private static final String SALE_IDS = "saleIds";

  private static final String SALE_ID = "saleId";
  private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
  private static final Map<PaymentMethod, String> METHOD_LABELS =
      Map.of(
          PaymentMethod.CASH, "Dinheiro",
          PaymentMethod.PIX, "Pix",
          PaymentMethod.DEBIT_CARD, "Cartão de débito",
          PaymentMethod.CREDIT_CARD, "Cartão de crédito");
  private static final String SELECT_SALE =
      """
      SELECT id, tipo, veiculo_id, cliente_nome, cliente_telefone, desconto,
             observacoes, criado_por, criado_em
      FROM vendas
      WHERE estabelecimento_id = :tenant
      """;

  private final Jdbi jdbi;

  public SaleJdbiRepository(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  @Override
  public Sale save(Sale sale) {
    jdbi.useTransaction(
        handle -> {
          handle
              .createUpdate(
                  """
                  INSERT INTO vendas
                    (id, estabelecimento_id, tipo, veiculo_id, cliente_nome, cliente_telefone,
                     subtotal, desconto, total, observacoes, criado_por, criado_em)
                  VALUES
                    (:id, :tenant, :type,
                     (SELECT v.id FROM veiculos v
                      WHERE v.id = CAST(:vehicleId AS UUID) AND v.estabelecimento_id = :tenant),
                     :customerName, :customerPhone, :subtotal, :discount,
                     :total, :notes, CAST(:createdBy AS UUID), :createdAt)
                  """)
              .bind("id", sale.id())
              .bind("type", sale.type().dbValue())
              .bind("vehicleId", sale.vehicleId())
              .bind("customerName", sale.customerName())
              .bind("customerPhone", sale.customerPhone())
              .bind("subtotal", sale.subtotal())
              .bind("discount", sale.discount())
              .bind(TOTAL, sale.total())
              .bind("notes", sale.notes())
              .bind("createdBy", sale.createdBy())
              .bind("createdAt", sale.createdAt())
              .execute();
          for (SaleItem item : sale.items()) {
            handle
                .createUpdate(
                    """
                    INSERT INTO venda_itens
                      (id, venda_id, servico_preco_id, descricao, quantidade, preco_unitario,
                       total)
                    VALUES (:id, :saleId, :servicePriceId, :description, :quantity, :unitPrice,
                            :total)
                    """)
                .bind("id", item.id())
                .bind(SALE_ID, sale.id())
                .bind("servicePriceId", item.servicePriceId())
                .bind("description", item.description())
                .bind("quantity", item.quantity())
                .bind("unitPrice", item.unitPrice())
                .bind(TOTAL, item.total())
                .execute();
          }
          for (SalePayment payment : sale.payments()) {
            handle
                .createUpdate(
                    """
                    INSERT INTO venda_pagamentos (id, venda_id, forma_pagamento, valor, recebido_em)
                    VALUES (:id, :saleId, :method, :amount, :receivedAt)
                    """)
                .bind("id", payment.id())
                .bind(SALE_ID, sale.id())
                .bind(METHOD, payment.method().name())
                .bind("amount", payment.amount())
                .bind("receivedAt", payment.receivedAt())
                .execute();
          }
        });
    return sale;
  }

  @Override
  public List<Sale> loadBetween(Instant start, Instant end, SaleType type) {
    return jdbi.withHandle(
        handle -> {
          List<SaleRow> rows =
              handle
                  .createQuery(
                      SELECT_SALE
                          + """
                      AND criado_em >= :start AND criado_em < :end
                        AND (:type IS NULL OR tipo = :type)
                      ORDER BY criado_em DESC
                      """)
                  .bind("start", start)
                  .bind("end", end)
                  .bind("type", type == null ? null : type.dbValue())
                  .map(SaleJdbiRepository::mapRow)
                  .list();
          if (rows.isEmpty()) {
            return List.of();
          }
          return assemble(handle, rows);
        });
  }

  // FOR UPDATE on the sale serializes refunds of the same sale: two concurrent partial refunds
  // can't both read the same balance and together give back more than was paid.
  @Override
  public Sale refund(UUID saleId, SaleRefund refund)
      throws SaleNotFoundError, SaleRefundNotAllowedError {
    try {
      return jdbi.inTransaction(
          handle -> {
            SaleRow row =
                handle
                    .createQuery(SELECT_SALE + " AND id = :id FOR UPDATE")
                    .bind("id", saleId)
                    .map(SaleJdbiRepository::mapRow)
                    .findOne()
                    .orElseThrow(() -> new Rejected(new SaleNotFoundError()));
            if (row.type() != SaleType.QUICK_SALE) {
              throw new Rejected(new SaleRefundNotAllowedError("Orçamento não tem estorno"));
            }
            BigDecimal available =
                handle
                    .createQuery(
                        """
                        SELECT
                          (SELECT COALESCE(SUM(valor), 0) FROM venda_pagamentos
                           WHERE venda_id = :id AND forma_pagamento = :method)
                          - (SELECT COALESCE(SUM(valor), 0) FROM venda_estornos
                             WHERE venda_id = :id AND forma_devolucao = :method)
                        """)
                    .bind("id", saleId)
                    .bind(METHOD, refund.method().name())
                    .mapTo(BigDecimal.class)
                    .one();
            if (refund.amount().compareTo(available) > 0) {
              throw new Rejected(
                  new SaleRefundNotAllowedError(
                      String.format(
                          PT_BR,
                          "%s só tem R$ %,.2f para estornar nesta venda",
                          METHOD_LABELS.get(refund.method()),
                          available)));
            }
            handle
                .createUpdate(
                    """
                    INSERT INTO venda_estornos
                      (id, estabelecimento_id, venda_id, forma_devolucao, valor, motivo,
                       descricao, criado_por, criado_em)
                    VALUES (:id, :tenant, :saleId, :method, :amount, :reason, :description,
                            :createdBy, :createdAt)
                    """)
                .bind("id", refund.id())
                .bind(SALE_ID, saleId)
                .bind(METHOD, refund.method().name())
                .bind("amount", refund.amount())
                .bind("reason", refund.reason())
                .bind("description", refund.description())
                .bind("createdBy", refund.createdBy())
                .bind("createdAt", refund.createdAt())
                .execute();
            return assemble(handle, List.of(row)).getFirst();
          });
    } catch (Rejected rejected) {
      if (rejected.getCause() instanceof SaleNotFoundError notFound) {
        throw notFound;
      }
      throw (SaleRefundNotAllowedError) rejected.getCause();
    }
  }

  private static List<Sale> assemble(Handle handle, List<SaleRow> rows) {
    List<UUID> ids = rows.stream().map(SaleRow::id).toList();
    Map<UUID, List<SaleItem>> items = loadItems(handle, ids);
    Map<UUID, List<SalePayment>> payments = loadPayments(handle, ids);
    Map<UUID, List<SaleRefund>> refunds = loadRefunds(handle, ids);
    return rows.stream()
        .map(
            row ->
                Sale.of(
                    row.id(),
                    row.type(),
                    row.vehicleId(),
                    row.customerName(),
                    row.customerPhone(),
                    row.discount(),
                    row.notes(),
                    row.createdBy(),
                    row.createdAt(),
                    items.getOrDefault(row.id(), List.of()),
                    payments.getOrDefault(row.id(), List.of()),
                    refunds.getOrDefault(row.id(), List.of())))
        .toList();
  }

  private static SaleRow mapRow(ResultSet rs, StatementContext ctx) throws SQLException {
    return new SaleRow(
        rs.getObject("id", UUID.class),
        SaleType.fromDbValue(rs.getString("tipo")),
        rs.getObject("veiculo_id", UUID.class),
        rs.getString("cliente_nome"),
        rs.getString("cliente_telefone"),
        rs.getBigDecimal("desconto"),
        rs.getString("observacoes"),
        rs.getObject(CRIADO_POR, UUID.class),
        rs.getTimestamp(CRIADO_EM).toInstant());
  }

  @Override
  public List<SaleRefund> loadRefundsBetween(Instant start, Instant end) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(
                    """
                    SELECT id, venda_id, forma_devolucao, valor, motivo, descricao, criado_por,
                           criado_em
                    FROM venda_estornos
                    WHERE estabelecimento_id = :tenant
                      AND criado_em >= :start AND criado_em < :end
                    ORDER BY criado_em DESC
                    """)
                .bind("start", start)
                .bind("end", end)
                .map(SaleJdbiRepository::mapRefund)
                .list());
  }

  private static SaleRefund mapRefund(ResultSet rs, StatementContext ctx) throws SQLException {
    return new SaleRefund(
        rs.getObject("id", UUID.class),
        rs.getObject(VENDA_ID, UUID.class),
        PaymentMethod.valueOf(rs.getString("forma_devolucao")),
        rs.getBigDecimal(VALOR),
        rs.getString("motivo"),
        rs.getString(DESCRICAO),
        rs.getObject(CRIADO_POR, UUID.class),
        rs.getTimestamp(CRIADO_EM).toInstant());
  }

  private static Map<UUID, List<SaleRefund>> loadRefunds(Handle handle, List<UUID> saleIds) {
    Map<UUID, List<SaleRefund>> bySale = new HashMap<>();
    handle
        .createQuery(
            """
            SELECT id, venda_id, forma_devolucao, valor, motivo, descricao, criado_por, criado_em
            FROM venda_estornos WHERE venda_id IN (<saleIds>)
            ORDER BY criado_em
            """)
        .bindList(SALE_IDS, saleIds)
        .map(
            (rs, ctx) -> {
              bySale
                  .computeIfAbsent(rs.getObject(VENDA_ID, UUID.class), id -> new ArrayList<>())
                  .add(
                      new SaleRefund(
                          rs.getObject("id", UUID.class),
                          rs.getObject(VENDA_ID, UUID.class),
                          PaymentMethod.valueOf(rs.getString("forma_devolucao")),
                          rs.getBigDecimal(VALOR),
                          rs.getString("motivo"),
                          rs.getString(DESCRICAO),
                          rs.getObject(CRIADO_POR, UUID.class),
                          rs.getTimestamp(CRIADO_EM).toInstant()));
              return 0;
            })
        .list();
    return bySale;
  }

  /** Carries a checked domain error out of the JDBI transaction callback (rolls it back). */
  private static final class Rejected extends RuntimeException {
    Rejected(Exception cause) {
      super(cause);
    }
  }

  private static Map<UUID, List<SaleItem>> loadItems(Handle handle, List<UUID> saleIds) {
    Map<UUID, List<SaleItem>> bySale = new HashMap<>();
    handle
        .createQuery(
            """
            SELECT id, venda_id, servico_preco_id, descricao, quantidade, preco_unitario, total
            FROM venda_itens WHERE venda_id IN (<saleIds>)
            """)
        .bindList(SALE_IDS, saleIds)
        .map(
            (rs, ctx) -> {
              bySale
                  .computeIfAbsent(rs.getObject(VENDA_ID, UUID.class), id -> new ArrayList<>())
                  .add(
                      new SaleItem(
                          rs.getObject("id", UUID.class),
                          rs.getInt("servico_preco_id"),
                          rs.getString(DESCRICAO),
                          rs.getInt("quantidade"),
                          rs.getBigDecimal("preco_unitario"),
                          rs.getBigDecimal(TOTAL)));
              return 0;
            })
        .list();
    return bySale;
  }

  private static Map<UUID, List<SalePayment>> loadPayments(Handle handle, List<UUID> saleIds) {
    Map<UUID, List<SalePayment>> bySale = new HashMap<>();
    handle
        .createQuery(
            """
            SELECT id, venda_id, forma_pagamento, valor, recebido_em
            FROM venda_pagamentos WHERE venda_id IN (<saleIds>)
            ORDER BY recebido_em
            """)
        .bindList(SALE_IDS, saleIds)
        .map(
            (rs, ctx) -> {
              bySale
                  .computeIfAbsent(rs.getObject(VENDA_ID, UUID.class), id -> new ArrayList<>())
                  .add(
                      new SalePayment(
                          rs.getObject("id", UUID.class),
                          PaymentMethod.valueOf(rs.getString("forma_pagamento")),
                          rs.getBigDecimal(VALOR),
                          rs.getTimestamp("recebido_em").toInstant()));
              return 0;
            })
        .list();
    return bySale;
  }

  private record SaleRow(
      UUID id,
      SaleType type,
      UUID vehicleId,
      String customerName,
      String customerPhone,
      BigDecimal discount,
      String notes,
      UUID createdBy,
      Instant createdAt) {}
}
