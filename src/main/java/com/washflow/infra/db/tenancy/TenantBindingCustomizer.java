package com.washflow.infra.db.tenancy;

import com.washflow.data.tenancy.TenantContext;
import java.sql.PreparedStatement;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.StatementCustomizer;

/**
 * Binds {@code :tenant} to the current establishment on every statement that uses it, unless the
 * caller bound it explicitly (platform admin flows). A statement that needs a tenant outside of one
 * throws {@link com.washflow.domain.errors.common.TenantRequiredError}.
 */
public final class TenantBindingCustomizer implements StatementCustomizer {

  public static final String PARAMETER = "tenant";
  private static final String PLACEHOLDER = ":" + PARAMETER;

  @Override
  public void beforeBinding(PreparedStatement statement, StatementContext ctx) {
    String sql = ctx.getRawSql();
    if (sql == null || !sql.contains(PLACEHOLDER)) {
      return;
    }
    if (ctx.getBinding().getNames().contains(PARAMETER)) {
      return;
    }
    ctx.getBinding().addNamed(PARAMETER, TenantContext.require());
  }
}
