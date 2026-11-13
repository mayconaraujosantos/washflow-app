package com.washflow.data.protocols.db.yard;

import com.washflow.domain.usecases.yard.GetWasherBoard;
import java.util.List;

public interface LoadWasherBoardRepository {

  /** Every active washer of the current lava-jato with the wash in progress, if any. */
  List<GetWasherBoard.WasherStatus> loadWasherBoard();
}
