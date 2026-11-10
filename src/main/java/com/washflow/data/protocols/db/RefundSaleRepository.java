package com.washflow.data.protocols.db;

import com.washflow.domain.entities.Sale;
import com.washflow.domain.entities.SaleRefund;
import com.washflow.domain.errors.sale.SaleNotFoundError;
import com.washflow.domain.errors.sale.SaleRefundNotAllowedError;
import java.util.UUID;

/**
 * Port for refunding a sale. Checks, under a lock on the sale, that it is a quick sale and that the
 * method still has {@code refund.amount()} to give back; returns the sale with its refunds.
 */
public interface RefundSaleRepository {

  Sale refund(UUID saleId, SaleRefund refund) throws SaleNotFoundError, SaleRefundNotAllowedError;
}
