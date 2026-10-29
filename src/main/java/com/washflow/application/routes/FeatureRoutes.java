package com.washflow.application.routes;

import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;
import static io.javalin.apibuilder.ApiBuilder.put;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.washflow.application.adapters.JavalinRouteAdapter;
import com.washflow.application.factories.controllers.DeletePhotoControllerFactory;
import com.washflow.application.factories.controllers.GetChecklistsControllerFactory;
import com.washflow.application.factories.controllers.GetPhotosControllerFactory;
import com.washflow.application.factories.controllers.SaveChecklistControllerFactory;
import com.washflow.application.factories.controllers.SavePhotoControllerFactory;
import com.washflow.application.security.AuthTokenService;
import com.washflow.domain.entities.CashTransaction;
import com.washflow.domain.entities.CashTransaction.TransactionType;
import com.washflow.domain.entities.ExpenseCategory;
import com.washflow.infra.db.jdbi.CashTransactionJdbiRepository;
import io.javalin.http.Context;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jdbi.v3.core.Jdbi;

public final class FeatureRoutes {

  private FeatureRoutes() {}

  public static void register(Jdbi jdbi) {
    CashTransactionJdbiRepository cashRepo = new CashTransactionJdbiRepository(jdbi);

    // --- Checklists (Clean Architecture pattern) ---
    post("/api/service-orders/{id}/checklist", JavalinRouteAdapter.adapt(SaveChecklistControllerFactory.make(jdbi)));
    get("/api/service-orders/{id}/checklist", JavalinRouteAdapter.adapt(GetChecklistsControllerFactory.make(jdbi)));

    // --- Photos (Clean Architecture pattern) ---
    post("/api/service-orders/{id}/photos", JavalinRouteAdapter.adapt(SavePhotoControllerFactory.make(jdbi)));
    get("/api/service-orders/{id}/photos", JavalinRouteAdapter.adapt(GetPhotosControllerFactory.make(jdbi)));
    delete("/api/service-orders/{id}/photos/{photoId}", JavalinRouteAdapter.adapt(DeletePhotoControllerFactory.make(jdbi)));

    // --- Cash Transactions ---
    post("/api/cash-transactions", ctx -> createTransaction(ctx, cashRepo));
    get("/api/cash-transactions", ctx -> listTransactions(ctx, cashRepo));
    put("/api/cash-transactions/{id}", ctx -> updateTransaction(ctx, cashRepo));
    delete("/api/cash-transactions/{id}", ctx -> deleteTransaction(ctx, cashRepo));
    get("/api/cash-transactions/report", ctx -> transactionReport(ctx, cashRepo));

    // --- Expense Categories ---
    post("/api/expense-categories", ctx -> createCategory(ctx, cashRepo));
    get("/api/expense-categories", ctx -> listCategories(ctx, cashRepo));
  }

  // ==================== CASH TRANSACTIONS ====================

  private static void createTransaction(Context ctx, CashTransactionJdbiRepository repo) {
    AuthTokenService.Claims claims = ctx.attribute("washflow.auth.claims");
    Map<String, Object> body = ctx.bodyAsClass(Map.class);

    String typeStr = (String) body.get("type");
    TransactionType type;
    try {
      type = TransactionType.valueOf(typeStr);
    } catch (IllegalArgumentException e) {
      ctx.status(400).json(Map.of("error", "Type must be INCOME or EXPENSE"));
      return;
    }

    String amountStr = (String) body.get("amount");
    BigDecimal amount;
    try {
      amount = new BigDecimal(amountStr);
    } catch (NumberFormatException e) {
      ctx.status(400).json(Map.of("error", "Invalid amount"));
      return;
    }

    String dateStr = (String) body.get("referenceDate");
    LocalDate referenceDate;
    try {
      referenceDate = LocalDate.parse(dateStr);
    } catch (DateTimeParseException e) {
      ctx.status(400).json(Map.of("error", "referenceDate must be YYYY-MM-DD"));
      return;
    }

    UUID categoryId = null;
    Object catId = body.get("categoryId");
    if (catId != null) {
      try {
        categoryId = UUID.fromString((String) catId);
      } catch (IllegalArgumentException e) {
        ctx.status(400).json(Map.of("error", "Invalid categoryId"));
        return;
      }
    }

    String description = (String) body.get("description");
    UUID serviceOrderId = null;
    Object soId = body.get("serviceOrderId");
    if (soId != null) {
      try {
        serviceOrderId = UUID.fromString((String) soId);
      } catch (IllegalArgumentException e) {
        ctx.status(400).json(Map.of("error", "Invalid serviceOrderId"));
        return;
      }
    }

    CashTransaction transaction =
        new CashTransaction(
            UUID.randomUUID(),
            type,
            amount,
            categoryId,
            description,
            referenceDate,
            serviceOrderId,
            Instant.now(),
            claims.userId());

    CashTransaction saved = repo.save(transaction);
    ctx.status(201).json(saved);
  }

  private static void listTransactions(Context ctx, CashTransactionJdbiRepository repo) {
    String fromStr = ctx.queryParam("from");
    String toStr = ctx.queryParam("to");

    List<CashTransaction> transactions;
    if (fromStr != null && toStr != null) {
      LocalDate from;
      LocalDate to;
      try {
        from = LocalDate.parse(fromStr);
        to = LocalDate.parse(toStr);
      } catch (DateTimeParseException e) {
        ctx.status(400).json(Map.of("error", "from and to must be YYYY-MM-DD"));
        return;
      }
      transactions = repo.loadByDateRange(from, to);
    } else {
      transactions = repo.loadAll();
    }

    ctx.json(transactions);
  }

  private static void updateTransaction(Context ctx, CashTransactionJdbiRepository repo) {
    UUID transactionId;
    try {
      transactionId = UUID.fromString(ctx.pathParam("id"));
    } catch (IllegalArgumentException e) {
      ctx.status(400).json(Map.of("error", "Invalid transaction id"));
      return;
    }

    CashTransaction existing = repo.loadById(transactionId);
    Map<String, Object> body = ctx.bodyAsClass(Map.class);

    TransactionType type = existing.type();
    Object typeObj = body.get("type");
    if (typeObj != null) {
      try {
        type = TransactionType.valueOf((String) typeObj);
      } catch (IllegalArgumentException e) {
        ctx.status(400).json(Map.of("error", "Type must be INCOME or EXPENSE"));
        return;
      }
    }

    BigDecimal amount = existing.amount();
    Object amountObj = body.get("amount");
    if (amountObj != null) {
      try {
        amount = new BigDecimal((String) amountObj);
      } catch (NumberFormatException e) {
        ctx.status(400).json(Map.of("error", "Invalid amount"));
        return;
      }
    }

    UUID categoryId = existing.categoryId();
    Object catIdObj = body.get("categoryId");
    if (catIdObj != null) {
      try {
        categoryId = UUID.fromString((String) catIdObj);
      } catch (IllegalArgumentException e) {
        ctx.status(400).json(Map.of("error", "Invalid categoryId"));
        return;
      }
    }

    String description = existing.description();
    Object descObj = body.get("description");
    if (descObj != null) {
      description = (String) descObj;
    }

    LocalDate referenceDate = existing.referenceDate();
    Object dateObj = body.get("referenceDate");
    if (dateObj != null) {
      try {
        referenceDate = LocalDate.parse((String) dateObj);
      } catch (DateTimeParseException e) {
        ctx.status(400).json(Map.of("error", "referenceDate must be YYYY-MM-DD"));
        return;
      }
    }

    UUID serviceOrderId = existing.serviceOrderId();
    Object soIdObj = body.get("serviceOrderId");
    if (soIdObj != null) {
      try {
        serviceOrderId = UUID.fromString((String) soIdObj);
      } catch (IllegalArgumentException e) {
        ctx.status(400).json(Map.of("error", "Invalid serviceOrderId"));
        return;
      }
    }

    CashTransaction updated =
        new CashTransaction(
            transactionId,
            type,
            amount,
            categoryId,
            description,
            referenceDate,
            serviceOrderId,
            existing.createdAt(),
            existing.createdBy());

    CashTransaction saved = repo.update(updated);
    ctx.json(saved);
  }

  private static void deleteTransaction(Context ctx, CashTransactionJdbiRepository repo) {
    UUID transactionId;
    try {
      transactionId = UUID.fromString(ctx.pathParam("id"));
    } catch (IllegalArgumentException e) {
      ctx.status(400).json(Map.of("error", "Invalid transaction id"));
      return;
    }

    repo.deleteById(transactionId);
    ctx.status(204).result("");
  }

  private static void transactionReport(Context ctx, CashTransactionJdbiRepository repo) {
    String fromStr = ctx.queryParam("from");
    String toStr = ctx.queryParam("to");

    if (fromStr == null || toStr == null) {
      ctx.status(400).json(Map.of("error", "Provide from and to as YYYY-MM-DD dates"));
      return;
    }

    LocalDate from;
    LocalDate to;
    try {
      from = LocalDate.parse(fromStr);
      to = LocalDate.parse(toStr);
    } catch (DateTimeParseException e) {
      ctx.status(400).json(Map.of("error", "Provide from and to as YYYY-MM-DD dates"));
      return;
    }

    if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > 365) {
      ctx.status(400)
          .json(Map.of("error", "Report range must be ordered and no longer than 366 days"));
      return;
    }

    Map<String, Object> report = repo.report(from, to);
    ctx.json(report);
  }

  // ==================== EXPENSE CATEGORIES ====================

  private static void createCategory(Context ctx, CashTransactionJdbiRepository repo) {
    Map<String, Object> body = ctx.bodyAsClass(Map.class);

    String name = (String) body.get("name");
    if (name == null || name.isBlank()) {
      ctx.status(400).json(Map.of("error", "name is required"));
      return;
    }

    String color = (String) body.get("color");
    if (color == null || color.isBlank()) {
      color = "#6B7280";
    }

    ExpenseCategory category =
        new ExpenseCategory(UUID.randomUUID(), name.trim(), color, Instant.now());

    ExpenseCategory saved = repo.save(category);
    ctx.status(201).json(saved);
  }

  private static void listCategories(Context ctx, CashTransactionJdbiRepository repo) {
    List<ExpenseCategory> categories = repo.loadAllCategories();
    ctx.json(categories);
  }
}
