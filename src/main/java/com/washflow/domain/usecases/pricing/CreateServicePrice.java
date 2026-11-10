package com.washflow.domain.usecases.pricing;

import com.washflow.domain.entities.ServicePrice;
import java.math.BigDecimal;

/** The manager adds a product/service to the {@code servicos_preco} catalog. */
public interface CreateServicePrice {

  ServicePrice create(Params params);

  record Params(String name, BigDecimal price, BigDecimal washerCommission) {}
}
