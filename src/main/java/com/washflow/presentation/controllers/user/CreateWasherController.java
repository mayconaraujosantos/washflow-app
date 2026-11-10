package com.washflow.presentation.controllers.user;

import com.washflow.domain.entities.user.User;
import com.washflow.domain.errors.user.PhoneAlreadyRegisteredError;
import com.washflow.domain.usecases.user.CreateWasher;
import com.washflow.presentation.helpers.BodyValues;
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
      Map<String, Object> body = request.body();
      String name = BodyValues.text(body, "name");
      String phone = BodyValues.text(body, "phone");
      String password = BodyValues.text(body, "password");

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
