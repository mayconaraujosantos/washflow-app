package com.washflow.domain.usecases;

import com.washflow.domain.entities.PaymentMethod;
import com.washflow.domain.entities.Sale;
import com.washflow.domain.entities.SaleType;
import com.washflow.domain.errors.cash.ServicePriceNotFoundError;
import com.washflow.domain.errors.vehicle.VehicleNotFoundError;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Registers a quick sale or a budget. Prices always come from the catalog, never from the client;
 * payments may be partial or split across methods but never exceed the total.
 */
public interface CreateSale {

  Sale create(Params params) throws ServicePriceNotFoundError, VehicleNotFoundError;

  record Params(
      SaleType type,
      UUID vehicleId,
      String customerName,
      String customerPhone,
      List<Item> items,
      BigDecimal discount,
      List<Payment> payments,
      String notes,
      UUID createdBy) {}

  record Item(int servicePriceId, int quantity) {}

  record Payment(PaymentMethod method, BigDecimal amount) {}
}
