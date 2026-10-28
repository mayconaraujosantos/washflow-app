package com.washflow.data.protocols.db;

import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.entities.ServicePayment;
import com.washflow.domain.entities.WasherCommission;

public interface FinalizeServiceOrderRepository {

  ServiceOrder finalizeSale(
      ServiceOrder serviceOrder, ServicePayment payment, WasherCommission commission);
}
