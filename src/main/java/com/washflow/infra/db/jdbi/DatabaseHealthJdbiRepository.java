package com.washflow.infra.db.jdbi;

import com.washflow.data.protocols.db.CheckDatabaseHealthRepository;
import org.jdbi.v3.core.Jdbi;

public class DatabaseHealthJdbiRepository implements CheckDatabaseHealthRepository {

  private final Jdbi jdbi;

  public DatabaseHealthJdbiRepository(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  @Override
  public boolean isUp() {
    try {
      jdbi.withHandle(handle -> handle.execute("SELECT 1"));
      return true;
    } catch (RuntimeException e) {
      return false;
    }
  }
}
