package com.washflow.data.usecases.yard;

import com.washflow.data.protocols.db.yard.LoadWasherBoardRepository;
import com.washflow.domain.usecases.yard.GetWasherBoard;
import java.util.Comparator;
import java.util.List;

public class DbGetWasherBoard implements GetWasherBoard {

  private final LoadWasherBoardRepository repository;

  public DbGetWasherBoard(LoadWasherBoardRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<WasherStatus> get() {
    return repository.loadWasherBoard().stream()
        .sorted(
            Comparator.comparing((WasherStatus washer) -> !WasherStatus.FREE.equals(washer.state()))
                .thenComparing(WasherStatus::name, String.CASE_INSENSITIVE_ORDER))
        .toList();
  }
}
