package com.washflow.data.usecases.createserviceprice;

import com.washflow.data.protocols.db.CreateServicePriceRepository;
import com.washflow.data.usecases.shared.ContactRules;
import com.washflow.domain.entities.ServicePrice;
import com.washflow.domain.usecases.pricing.CreateServicePrice;
import java.math.BigDecimal;

public class DbCreateServicePrice implements CreateServicePrice {

  private final CreateServicePriceRepository createServicePriceRepository;

  public DbCreateServicePrice(CreateServicePriceRepository createServicePriceRepository) {
    this.createServicePriceRepository = createServicePriceRepository;
  }

  @Override
  public ServicePrice create(Params params) {
    String name = ContactRules.limited(params.name(), 50, "Nome");
    if (name == null || name.length() < 2) {
      throw new IllegalArgumentException("Informe o nome do produto ou serviço");
    }
    BigDecimal price = params.price();
    if (price == null || price.signum() <= 0 || price.stripTrailingZeros().scale() > 2) {
      throw new IllegalArgumentException("Preço deve ser maior que zero, com até 2 casas");
    }
    BigDecimal commission =
        params.washerCommission() == null ? BigDecimal.ZERO : params.washerCommission();
    if (commission.signum() < 0
        || commission.compareTo(price) > 0
        || commission.stripTrailingZeros().scale() > 2) {
      throw new IllegalArgumentException("Comissão deve ficar entre zero e o preço");
    }
    return createServicePriceRepository.create(name, price.setScale(2), commission.setScale(2));
  }
}
