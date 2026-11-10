package com.washflow.presentation.controllers.schedule;

import com.washflow.domain.usecases.schedule.ListAgenda;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** {@code GET /api/operations/agenda?from=YYYY-MM-DD&to=YYYY-MM-DD} (shop-local dates). */
public class ListAgendaController implements Controller {

  private static final String INVALID_RANGE = "Provide from and to as YYYY-MM-DD dates";

  private final ListAgenda listAgenda;

  public ListAgendaController(ListAgenda listAgenda) {
    this.listAgenda = listAgenda;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      String from = request.queryParams().get("from");
      String to = request.queryParams().get("to");
      if (from == null || to == null) {
        return HttpHelper.badRequest(new IllegalArgumentException(INVALID_RANGE));
      }
      return HttpHelper.ok(
          listAgenda.list(new ListAgenda.Params(LocalDate.parse(from), LocalDate.parse(to))));
    } catch (DateTimeParseException e) {
      return HttpHelper.badRequest(new IllegalArgumentException(INVALID_RANGE));
    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
