package com.washflow.presentation.controllers.establishment;

import com.washflow.domain.errors.establishment.EstablishmentAlreadyConfiguredError;
import com.washflow.domain.errors.establishment.InvalidSetupCodeError;
import com.washflow.domain.usecases.SetupEstablishment;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.Map;

/**
 * {@code POST /api/setup}: returns the new owner {@code User}, which the authenticated route
 * adapter turns into a signed session so the owner lands logged in.
 */
public class SetupEstablishmentController implements Controller {

  private final SetupEstablishment setupEstablishment;

  public SetupEstablishmentController(SetupEstablishment setupEstablishment) {
    this.setupEstablishment = setupEstablishment;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      Map<String, Object> body = request.body() == null ? Map.of() : request.body();
      return HttpHelper.ok(
          setupEstablishment.setup(
              new SetupEstablishment.Params(
                  BodyValues.text(body, "setupCode"),
                  BodyValues.text(body, "ownerName"),
                  BodyValues.text(body, "ownerEmail"),
                  BodyValues.text(body, "ownerPassword"))));
    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (InvalidSetupCodeError e) {
      return HttpHelper.forbidden(e);
    } catch (EstablishmentAlreadyConfiguredError e) {
      return HttpHelper.conflict(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
