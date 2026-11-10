package com.washflow.data.protocols.db.user;

import com.washflow.domain.entities.WasherCommission;

public interface CreateWasherCommissionRepository {

  WasherCommission create(WasherCommission commission);
}
