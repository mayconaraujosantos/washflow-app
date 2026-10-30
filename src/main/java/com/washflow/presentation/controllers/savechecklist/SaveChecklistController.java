package com.washflow.presentation.controllers.savechecklist;

import com.washflow.domain.entities.ServiceOrderChecklist.ChecklistItem;
import com.washflow.domain.entities.ServiceOrderChecklist.ChecklistType;
import com.washflow.domain.errors.InvalidChecklistTypeError;
import com.washflow.domain.usecases.SaveServiceOrderChecklist;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Parses the HTTP request, validates fields, and delegates to {@code SaveServiceOrderChecklist}.
 * Framework-agnostic — depends only on HttpRequest/HttpResponse and the UseCase interface.
 *
 * <p>The {@code createdBy} UUID is expected in the request body under the key {@code "createdBy"},
 * injected by {@code JavalinRouteAdapter} from the authenticated user's claims.
 */
public class SaveChecklistController implements Controller {

  private final SaveServiceOrderChecklist saveServiceOrderChecklist;

  public SaveChecklistController(SaveServiceOrderChecklist saveServiceOrderChecklist) {
    this.saveServiceOrderChecklist = saveServiceOrderChecklist;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      UUID serviceOrderId = UUID.fromString(request.pathParams().get("id"));

      @SuppressWarnings("unchecked")
      Map<String, Object> body = request.body();

      String typeStr = (String) body.get("type");
      if (typeStr == null || typeStr.isBlank()) {
        return HttpHelper.badRequest(new IllegalArgumentException("type is required"));
      }
      ChecklistType type = ChecklistType.valueOf(typeStr);

      @SuppressWarnings("unchecked")
      List<Map<String, Object>> itemsMap = (List<Map<String, Object>>) body.get("items");
      if (itemsMap == null || itemsMap.isEmpty()) {
        return HttpHelper.badRequest(new IllegalArgumentException("items are required"));
      }
      List<ChecklistItem> items =
          itemsMap.stream()
              .map(
                  m ->
                      new ChecklistItem(
                          (String) m.get("label"),
                          Boolean.TRUE.equals(m.get("checked")),
                          (String) m.get("note")))
              .toList();

      String signatureImage = (String) body.get("signatureImage");

      UUID createdBy = (UUID) body.get("createdBy");
      if (createdBy == null) {
        return HttpHelper.badRequest(new IllegalArgumentException("createdBy is required"));
      }

      SaveServiceOrderChecklist.Params params =
          new SaveServiceOrderChecklist.Params(
              serviceOrderId, type, items, signatureImage, createdBy);

      var saved = saveServiceOrderChecklist.save(params);
      return new HttpResponse(201, saved);

    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (InvalidChecklistTypeError e) {
      return HttpHelper.badRequest(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
