package com.washflow.infra.db.jdbi;

import com.washflow.data.protocols.db.CashTransactionReportRepository;
import com.washflow.data.protocols.db.DeleteCashTransactionRepository;
import com.washflow.data.protocols.db.LoadCashTransactionRepository;
import com.washflow.data.protocols.db.LoadExpenseCategoryRepository;
import com.washflow.data.protocols.db.SaveCashTransactionRepository;
import com.washflow.data.protocols.db.SaveExpenseCategoryRepository;
import com.washflow.data.protocols.db.UpdateCashTransactionRepository;
import com.washflow.domain.entities.CashTransaction;
import com.washflow.domain.entities.CashTransaction.TransactionType;
import com.washflow.domain.entities.ExpenseCategory;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.StatementContext;

public class CashTransactionJdbiRepository
    implements SaveCashTransactionRepository,
        UpdateCashTransactionRepository,
        DeleteCashTransactionRepository,
        LoadCashTransactionRepository,
        SaveExpenseCategoryRepository,
        LoadExpenseCategoryRepository,
        CashTransactionReportRepository {

  private static final String INSERT_TRANSACTION_SQL =
      """
      INSERT INTO cash_transaction
        (id, tipo, valor, categoria_id, descricao, data_referencia, atendimento_id, criado_em, criado_por)
      VALUES
        (:id, :type, :amount, :categoryId, :description, :referenceDate, :serviceOrderId, :createdAt, :createdBy)
      """;

  private static final String UPDATE_TRANSACTION_SQL =
      """
      UPDATE cash_transaction
      SET tipo = :type, valor = :amount, categoria_id = :categoryId,
          descricao = :description, data_referencia = :referenceDate, atendimento_id = :serviceOrderId
      WHERE id = :id
      """;

  private static final String DELETE_TRANSACTION_SQL =
      "DELETE FROM cash_transaction WHERE id = :id";

  private static final String SELECT_BY_DATE_RANGE_SQL =
      """
      SELECT id, tipo, valor, categoria_id, descricao, data_referencia, atendimento_id, criado_em, criado_por
      FROM cash_transaction
      WHERE data_referencia >= :from AND data_referencia <= :to
      ORDER BY data_referencia DESC, criado_em DESC
      """;

  private static final String SELECT_ALL_SQL =
      """
      SELECT id, tipo, valor, categoria_id, descricao, data_referencia, atendimento_id, criado_em, criado_por
      FROM cash_transaction
      ORDER BY data_referencia DESC, criado_em DESC
      """;

  private static final String SELECT_BY_ID_SQL =
      """
      SELECT id, tipo, valor, categoria_id, descricao, data_referencia, atendimento_id, criado_em, criado_por
      FROM cash_transaction
      WHERE id = :id
      """;

  private static final String INSERT_CATEGORY_SQL =
      """
      INSERT INTO expense_category (id, nome, cor, criado_em)
      VALUES (:id, :name, :color, :createdAt)
      """;

  private static final String SELECT_ALL_CATEGORIES_SQL =
      "SELECT id, nome, cor, criado_em FROM expense_category ORDER BY nome";

  private static final String REPORT_SQL =
      """
      SELECT
        COALESCE(SUM(CASE WHEN tipo = 'INCOME' THEN valor ELSE 0 END), 0) as total_income,
        COALESCE(SUM(CASE WHEN tipo = 'EXPENSE' THEN valor ELSE 0 END), 0) as total_expense,
        COUNT(*) as total_count
      FROM cash_transaction
      WHERE data_referencia >= :from AND data_referencia <= :to
      """;

  private static final String REPORT_BY_CATEGORY_SQL =
      """
      SELECT ec.nome as category_name, ec.cor as category_color,
             ct.tipo as transaction_type,
             COALESCE(SUM(ct.valor), 0) as total,
             COUNT(*) as count
      FROM cash_transaction ct
      JOIN expense_category ec ON ec.id = ct.categoria_id
      WHERE ct.data_referencia >= :from AND ct.data_referencia <= :to
      GROUP BY ec.nome, ec.cor, ct.tipo
      ORDER BY total DESC
      """;

  private final Jdbi jdbi;

  public CashTransactionJdbiRepository(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  private CashTransaction mapTransactionRow(ResultSet rs, StatementContext ctx)
      throws SQLException {
    UUID id = UUID.fromString(rs.getString("id"));
    TransactionType type = TransactionType.valueOf(rs.getString("tipo"));
    BigDecimal amount = rs.getBigDecimal("valor");
    UUID categoryId = rs.getObject("categoria_id", UUID.class);
    String description = rs.getString("descricao");
    LocalDate referenceDate = rs.getDate("data_referencia").toLocalDate();
    UUID serviceOrderId = rs.getObject("atendimento_id", UUID.class);
    Instant createdAt = rs.getTimestamp("criado_em").toInstant();
    UUID createdBy = rs.getObject("criado_por", UUID.class);
    return new CashTransaction(
        id,
        type,
        amount,
        categoryId,
        description,
        referenceDate,
        serviceOrderId,
        createdAt,
        createdBy);
  }

  private ExpenseCategory mapCategoryRow(ResultSet rs, StatementContext ctx) throws SQLException {
    UUID id = UUID.fromString(rs.getString("id"));
    String name = rs.getString("nome");
    String color = rs.getString("cor");
    Instant createdAt = rs.getTimestamp("criado_em").toInstant();
    return new ExpenseCategory(id, name, color, createdAt);
  }

  @Override
  public CashTransaction save(CashTransaction transaction) {
    jdbi.useHandle(
        handle -> {
          handle
              .createUpdate(INSERT_TRANSACTION_SQL)
              .bind("id", transaction.id())
              .bind("type", transaction.type().name())
              .bind("amount", transaction.amount())
              .bind("categoryId", transaction.categoryId())
              .bind("description", transaction.description())
              .bind("referenceDate", transaction.referenceDate())
              .bind("serviceOrderId", transaction.serviceOrderId())
              .bind("createdAt", transaction.createdAt())
              .bind("createdBy", transaction.createdBy())
              .execute();
        });
    return transaction;
  }

  @Override
  public CashTransaction update(CashTransaction transaction) {
    jdbi.useHandle(
        handle -> {
          handle
              .createUpdate(UPDATE_TRANSACTION_SQL)
              .bind("id", transaction.id())
              .bind("type", transaction.type().name())
              .bind("amount", transaction.amount())
              .bind("categoryId", transaction.categoryId())
              .bind("description", transaction.description())
              .bind("referenceDate", transaction.referenceDate())
              .bind("serviceOrderId", transaction.serviceOrderId())
              .execute();
        });
    return transaction;
  }

  @Override
  public void deleteById(UUID transactionId) {
    jdbi.useHandle(
        handle -> handle.createUpdate(DELETE_TRANSACTION_SQL).bind("id", transactionId).execute());
  }

  @Override
  public List<CashTransaction> loadByDateRange(LocalDate from, LocalDate to) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(SELECT_BY_DATE_RANGE_SQL)
                .bind("from", from)
                .bind("to", to)
                .map(this::mapTransactionRow)
                .list());
  }

  @Override
  public List<CashTransaction> loadAll() {
    return jdbi.withHandle(
        handle -> handle.createQuery(SELECT_ALL_SQL).map(this::mapTransactionRow).list());
  }

  @Override
  public CashTransaction loadById(UUID transactionId) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(SELECT_BY_ID_SQL)
                .bind("id", transactionId)
                .map(this::mapTransactionRow)
                .one());
  }

  @Override
  public ExpenseCategory save(ExpenseCategory category) {
    jdbi.useHandle(
        handle -> {
          handle
              .createUpdate(INSERT_CATEGORY_SQL)
              .bind("id", category.id())
              .bind("name", category.name())
              .bind("color", category.color())
              .bind("createdAt", category.createdAt())
              .execute();
        });
    return category;
  }

  @Override
  public List<ExpenseCategory> loadAllCategories() {
    return jdbi.withHandle(
        handle -> handle.createQuery(SELECT_ALL_CATEGORIES_SQL).map(this::mapCategoryRow).list());
  }

  @Override
  public Map<String, Object> report(LocalDate from, LocalDate to) {
    Map<String, Object> result = new HashMap<>();
    result.put("from", from.toString());
    result.put("to", to.toString());

    Map<String, Object> summary =
        jdbi.withHandle(
            handle ->
                handle.createQuery(REPORT_SQL).bind("from", from).bind("to", to).mapToMap().one());

    @SuppressWarnings("unchecked")
    BigDecimal totalIncome = new BigDecimal(summary.get("total_income").toString());
    @SuppressWarnings("unchecked")
    BigDecimal totalExpense = new BigDecimal(summary.get("total_expense").toString());
    @SuppressWarnings("unchecked")
    int totalCount = Integer.parseInt(summary.get("total_count").toString());

    result.put("totalIncome", totalIncome);
    result.put("totalExpense", totalExpense);
    result.put("netBalance", totalIncome.subtract(totalExpense));
    result.put("totalCount", totalCount);

    List<Map<String, Object>> byCategory =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery(REPORT_BY_CATEGORY_SQL)
                    .bind("from", from)
                    .bind("to", to)
                    .mapToMap()
                    .list());

    result.put("byCategory", byCategory);

    return result;
  }
}
