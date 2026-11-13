package com.washflow.domain.usecases.yard;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The manager's board of active washers: who is free, who is washing which car (and since when),
 * who is on a pause, and how many waiting cars are reserved for each. Implemented by {@code
 * DbGetWasherBoard} in the data layer.
 */
public interface GetWasherBoard {

  /** Free washers first, then busy ones; by name. */
  List<WasherStatus> get();

  /** {@code state}: "FREE", "WASHING" or "PAUSED"; {@code current} is null when free. */
  record WasherStatus(
      UUID id, String name, String phone, String state, CurrentWash current, int reserved) {

    public static final String FREE = "FREE";
    public static final String WASHING = "WASHING";
    public static final String PAUSED = "PAUSED";
  }

  /**
   * {@code since}: when the wash started; {@code pauseReason} and {@code pausedAt} only while
   * paused ("LUNCH", "RESTROOM" or "OTHER").
   */
  record CurrentWash(
      UUID serviceOrderId,
      String vehiclePlate,
      String vehicleModel,
      Instant since,
      String pauseReason,
      Instant pausedAt) {}
}
