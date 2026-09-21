package com.washflow.data.protocols.db;

import java.time.Instant;

public interface CountServiceOrdersScheduledInRangeRepository {

  int countScheduledBetween(Instant start, Instant end);
}
