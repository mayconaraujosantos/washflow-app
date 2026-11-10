package com.washflow.data.protocols.db.user;

import com.washflow.domain.entities.serviceorder.WasherCommission;

public interface CreateWasherCommissionRepository {

  WasherCommission create(WasherCommission commission);
}
