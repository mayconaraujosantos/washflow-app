package com.washflow.infra.db.jdbi;

import com.washflow.data.protocols.db.shared.DuplicateKeyError;
import java.sql.SQLException;
import java.util.function.Supplier;
import org.jdbi.v3.core.JdbiException;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;

/** Turns PostgreSQL's unique_violation (SQLState 23505) into {@link DuplicateKeyError}. */
final class UniqueViolations {

  private static final String UNIQUE_VIOLATION = "23505";

  private UniqueViolations() {}

  static <T> T translate(Supplier<T> work) {
    try {
      return work.get();
    } catch (JdbiException e) {
      SQLException sql = findSqlException(e);
      if (sql != null && UNIQUE_VIOLATION.equals(sql.getSQLState())) {
        throw new DuplicateKeyError(constraint(sql), e);
      }
      throw e;
    }
  }

  static void translate(Runnable work) {
    translate(
        () -> {
          work.run();
          return null;
        });
  }

  private static SQLException findSqlException(Throwable error) {
    for (Throwable current = error; current != null; current = current.getCause()) {
      if (current instanceof SQLException sql) {
        return sql;
      }
    }
    return null;
  }

  private static String constraint(SQLException sql) {
    if (sql instanceof PSQLException psql) {
      ServerErrorMessage message = psql.getServerErrorMessage();
      if (message != null) {
        return message.getConstraint();
      }
    }
    return null;
  }
}
