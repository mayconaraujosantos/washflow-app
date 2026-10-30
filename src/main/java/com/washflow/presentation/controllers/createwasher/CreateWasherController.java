package com.washflow.presentation.controllers.createwasher;

import com.washflow.domain.entities.User;
import com.washflow.domain.errors.PhoneAlreadyRegisteredError;
import com.washflow.domain.usecases.CreateWasher;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.Map;

/**
 * Parses the HTTP request for creating a washer and delegates to {@code CreateWasher}.
 * Framework-agnostic — depends only on HttpRequest/HttpResponse and the UseCase interface.
 */
public class CreateWasherController implements Controller {

  private final CreateWasher createWasher;

  public CreateWasherController(CreateWasher createWasher) {
    this.createWasher = createWasher;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      @SuppressWarnings("unchecked")
      Map<String, Object> body = request.body();
      String name = (String) body.get("name");
      String phone = (String) body.get("phone");
      String password = (String) body.get("password");

      CreateWasher.Params params = new CreateWasher.Params(name, phone, password);

      User user = createWasher.create(params);
      return new HttpResponse(201, user);

    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (PhoneAlreadyRegisteredError e) {
      return new HttpResponse(409, Map.of("error", e.getMessage()));
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
