package com.washflow.infra.db.jdbi;

import com.washflow.data.protocols.db.LoadSalesRepository;
import com.washflow.data.protocols.db.SaveSaleRepository;
import com.washflow.domain.entities.PaymentMethod;
import com.washflow.domain.entities.Sale;
import com.washflow.domain.entities.SaleItem;
import com.washflow.domain.entities.SalePayment;
import com.washflow.domain.entities.SaleType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

public class SaleJdbiRepository implements SaveSaleRepository, LoadSalesRepository {

  private static final String SALE_ID = "saleId";

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
              .bind("total", sale.total())
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
                .bind("total", item.total())
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
                .bind("method", payment.method().name())
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
                      """
                      SELECT id, tipo, veiculo_id, cliente_nome, cliente_telefone, desconto,
                             observacoes, criado_por, criado_em
                      FROM vendas
                      WHERE estabelecimento_id = :tenant
                        AND criado_em >= :start AND criado_em < :end
                        AND (:type IS NULL OR tipo = :type)
                      ORDER BY criado_em DESC
                      """)
                  .bind("start", start)
                  .bind("end", end)
                  .bind("type", type == null ? null : type.dbValue())
                  .map(
                      (rs, ctx) ->
                          new SaleRow(
                              rs.getObject("id", UUID.class),
                              SaleType.fromDbValue(rs.getString("tipo")),
                              rs.getObject("veiculo_id", UUID.class),
                              rs.getString("cliente_nome"),
                              rs.getString("cliente_telefone"),
                              rs.getBigDecimal("desconto"),
                              rs.getString("observacoes"),
                              rs.getObject("criado_por", UUID.class),
                              rs.getTimestamp("criado_em").toInstant()))
                  .list();
          if (rows.isEmpty()) {
            return List.of();
          }
          List<UUID> ids = rows.stream().map(SaleRow::id).toList();
          Map<UUID, List<SaleItem>> items = loadItems(handle, ids);
          Map<UUID, List<SalePayment>> payments = loadPayments(handle, ids);
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
                          payments.getOrDefault(row.id(), List.of())))
              .toList();
        });
  }

  private static Map<UUID, List<SaleItem>> loadItems(Handle handle, List<UUID> saleIds) {
    Map<UUID, List<SaleItem>> bySale = new HashMap<>();
    handle
        .createQuery(
            """
            SELECT id, venda_id, servico_preco_id, descricao, quantidade, preco_unitario, total
            FROM venda_itens WHERE venda_id IN (<saleIds>)
            """)
        .bindList("saleIds", saleIds)
        .map(
            (rs, ctx) -> {
              bySale
                  .computeIfAbsent(rs.getObject("venda_id", UUID.class), id -> new ArrayList<>())
                  .add(
                      new SaleItem(
                          rs.getObject("id", UUID.class),
                          rs.getInt("servico_preco_id"),
                          rs.getString("descricao"),
                          rs.getInt("quantidade"),
                          rs.getBigDecimal("preco_unitario"),
                          rs.getBigDecimal("total")));
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
        .bindList("saleIds", saleIds)
        .map(
            (rs, ctx) -> {
              bySale
                  .computeIfAbsent(rs.getObject("venda_id", UUID.class), id -> new ArrayList<>())
                  .add(
                      new SalePayment(
                          rs.getObject("id", UUID.class),
                          PaymentMethod.valueOf(rs.getString("forma_pagamento")),
                          rs.getBigDecimal("valor"),
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
      java.math.BigDecimal discount,
      String notes,
      UUID createdBy,
      Instant createdAt) {}
}
