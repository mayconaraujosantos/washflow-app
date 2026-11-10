package com.washflow.data.protocols.db.shared;

import com.washflow.domain.entities.ServicePrice;
import java.math.BigDecimal;

public interface CreateServicePriceRepository {

  ServicePrice create(String name, BigDecimal price, BigDecimal washerCommission);
}
