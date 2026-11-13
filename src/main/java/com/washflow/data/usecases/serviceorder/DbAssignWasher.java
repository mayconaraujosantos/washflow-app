package com.washflow.data.usecases.serviceorder;

import com.washflow.data.protocols.db.serviceorder.AssignWasherRepository;
import com.washflow.data.protocols.db.serviceorder.LoadServiceOrderByIdRepository;
import com.washflow.data.protocols.db.user.LoadUserByIdRepository;
import com.washflow.domain.entities.serviceorder.ServiceOrder;
import com.washflow.domain.entities.serviceorder.ServiceOrderStatus;
import com.washflow.domain.entities.user.UserProfile;
import com.washflow.domain.errors.serviceorder.ServiceOrderNotAssignableError;
import com.washflow.domain.errors.serviceorder.ServiceOrderNotFoundError;
import com.washflow.domain.errors.serviceorder.WasherNotAssignableError;
import com.washflow.domain.usecases.serviceorder.AssignWasher;
import java.util.Set;

public class DbAssignWasher implements AssignWasher {

  private static final Set<ServiceOrderStatus> BEFORE_WASHING =
      Set.of(ServiceOrderStatus.SCHEDULED, ServiceOrderStatus.WAITING_IN_YARD);

  private final LoadServiceOrderByIdRepository loadServiceOrderByIdRepository;
  private final AssignWasherRepository assignWasherRepository;
  private final LoadUserByIdRepository loadUserByIdRepository;

  public DbAssignWasher(
      LoadServiceOrderByIdRepository loadServiceOrderByIdRepository,
      AssignWasherRepository assignWasherRepository,
      LoadUserByIdRepository loadUserByIdRepository) {
    this.loadServiceOrderByIdRepository = loadServiceOrderByIdRepository;
    this.assignWasherRepository = assignWasherRepository;
    this.loadUserByIdRepository = loadUserByIdRepository;
  }

  @Override
  public ServiceOrder assign(Params params) {
    if (params.washerId() != null
        && loadUserByIdRepository
            .loadById(params.washerId())
            .filter(user -> user.profile() == UserProfile.WASHER && user.active())
            .isEmpty()) {
      throw new WasherNotAssignableError();
    }
    ServiceOrder order = load(params);
    if (!BEFORE_WASHING.contains(order.status())
        || !assignWasherRepository.assignWasher(params.serviceOrderId(), params.washerId())) {
      throw new ServiceOrderNotAssignableError();
    }
    return load(params);
  }

  private ServiceOrder load(Params params) {
    return loadServiceOrderByIdRepository
        .loadById(params.serviceOrderId())
        .orElseThrow(() -> new ServiceOrderNotFoundError(params.serviceOrderId()));
  }
}
