package com.washflow.data.usecases.createsale;

import com.washflow.data.protocols.db.LoadServicePriceByIdRepository;
import com.washflow.data.protocols.db.LoadVehicleByIdRepository;
import com.washflow.data.protocols.db.SaveSaleRepository;
import com.washflow.data.usecases.shared.ContactRules;
import com.washflow.domain.entities.Sale;
import com.washflow.domain.entities.SaleItem;
import com.washflow.domain.entities.SalePayment;
import com.washflow.domain.entities.ServicePrice;
import com.washflow.domain.errors.ServicePriceNotFoundError;
import com.washflow.domain.errors.VehicleNotFoundError;
import com.washflow.domain.usecases.CreateSale;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Only talks to {@code data.protocols.db} ports; totals are computed here, not by the client. */
public class DbCreateSale implements CreateSale {

  private static final int MAX_ITEMS = 30;
  private static final int MAX_QUANTITY = 99;
  private static final int MAX_PAYMENTS = 5;

  private final LoadServicePriceByIdRepository loadServicePriceByIdRepository;
  private final LoadVehicleByIdRepository loadVehicleByIdRepository;
  private final SaveSaleRepository saveSaleRepository;
  private final Clock clock;

  public DbCreateSale(
      LoadServicePriceByIdRepository loadServicePriceByIdRepository,
      LoadVehicleByIdRepository loadVehicleByIdRepository,
      SaveSaleRepository saveSaleRepository,
      Clock clock) {
    this.loadServicePriceByIdRepository = loadServicePriceByIdRepository;
    this.loadVehicleByIdRepository = loadVehicleByIdRepository;
    this.saveSaleRepository = saveSaleRepository;
    this.clock = clock;
  }

  @Override
  public Sale create(Params params) throws ServicePriceNotFoundError, VehicleNotFoundError {
    if (params.type() == null) {
      throw new IllegalArgumentException("Informe se é venda rápida ou orçamento");
    }
    String customerName = ContactRules.limited(params.customerName(), 100, "Nome");
    String customerPhone = ContactRules.normalizePhone(params.customerPhone());
    String notes = ContactRules.limited(params.notes(), 500, "Observações");
    if (params.vehicleId() != null) {
      loadVehicleByIdRepository
          .loadById(params.vehicleId())
          .orElseThrow(() -> new VehicleNotFoundError(params.vehicleId()));
    }

    List<SaleItem> items = buildItems(params.items());
    BigDecimal discount = money(params.discount() == null ? BigDecimal.ZERO : params.discount());
    BigDecimal subtotal =
        items.stream().map(SaleItem::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    if (discount.signum() < 0 || discount.compareTo(subtotal) > 0) {
      throw new IllegalArgumentException("Desconto deve ficar entre zero e o subtotal");
    }

    Instant now = Instant.now(clock);
    List<SalePayment> payments = buildPayments(params.payments(), now);
    BigDecimal paid =
        payments.stream().map(SalePayment::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    if (paid.compareTo(subtotal.subtract(discount)) > 0) {
      throw new IllegalArgumentException("Pagamentos excedem o total");
    }

    return saveSaleRepository.save(
        Sale.of(
            UUID.randomUUID(),
            params.type(),
            params.vehicleId(),
            customerName,
            customerPhone,
            discount,
            notes,
            params.createdBy(),
            now,
            items,
            payments));
  }

  private List<SaleItem> buildItems(List<Item> requested) throws ServicePriceNotFoundError {
    if (requested == null || requested.isEmpty()) {
      throw new IllegalArgumentException("Adicione pelo menos um produto ou serviço");
    }
    if (requested.size() > MAX_ITEMS) {
      throw new IllegalArgumentException("No máximo " + MAX_ITEMS + " itens");
    }
    List<SaleItem> items = new ArrayList<>();
    for (Item item : requested) {
      if (item.quantity() < 1 || item.quantity() > MAX_QUANTITY) {
        throw new IllegalArgumentException("Quantidade deve ficar entre 1 e " + MAX_QUANTITY);
      }
      ServicePrice price =
          loadServicePriceByIdRepository
              .loadById(item.servicePriceId())
              .orElseThrow(() -> new ServicePriceNotFoundError(item.servicePriceId()));
      items.add(
          new SaleItem(
              UUID.randomUUID(),
              price.id(),
              price.name(),
              item.quantity(),
              price.price(),
              money(price.price().multiply(BigDecimal.valueOf(item.quantity())))));
    }
    return items;
  }

  private static List<SalePayment> buildPayments(List<Payment> requested, Instant now) {
    if (requested == null) {
      return List.of();
    }
    if (requested.size() > MAX_PAYMENTS) {
      throw new IllegalArgumentException("No máximo " + MAX_PAYMENTS + " pagamentos");
    }
    List<SalePayment> payments = new ArrayList<>();
    for (Payment payment : requested) {
      if (payment.method() == null) {
        throw new IllegalArgumentException("Forma de pagamento obrigatória");
      }
      BigDecimal amount = money(payment.amount());
      if (amount.signum() <= 0) {
        throw new IllegalArgumentException("Valor do pagamento deve ser maior que zero");
      }
      payments.add(new SalePayment(UUID.randomUUID(), payment.method(), amount, now));
    }
    return payments;
  }

  private static BigDecimal money(BigDecimal value) {
    if (value == null) {
      throw new IllegalArgumentException("Valor obrigatório");
    }
    if (value.stripTrailingZeros().scale() > 2) {
      throw new IllegalArgumentException("Valores aceitam no máximo 2 casas decimais");
    }
    return value.setScale(2);
  }
}
