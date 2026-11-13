package com.washflow.presentation.controllers.yard;

import com.washflow.domain.usecases.yard.GetWasherBoard;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;

/** {@code GET /api/operations/washers} (manager): who is free, washing or on a pause. */
public class GetWasherBoardController implements Controller {

  private final GetWasherBoard getWasherBoard;

  public GetWasherBoardController(GetWasherBoard getWasherBoard) {
    this.getWasherBoard = getWasherBoard;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      return HttpHelper.ok(getWasherBoard.get());
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
