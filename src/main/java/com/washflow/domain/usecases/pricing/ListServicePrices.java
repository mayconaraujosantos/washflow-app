package com.washflow.domain.usecases.pricing;

import com.washflow.domain.entities.pricing.ServicePrice;
import java.util.List;

/**
 * Lists the wash-type catalog ({@code servicos_preco}) - backs RF-03's service picker from {@code
 * docs/feature_espec_visao_do_cliente.md}. Implemented by {@code DbListServicePrices} in the data
 * layer.
 */
public interface ListServicePrices {

  List<ServicePrice> list();
}
