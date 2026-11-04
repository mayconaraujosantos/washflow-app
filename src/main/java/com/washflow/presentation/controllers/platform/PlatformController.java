package com.washflow.presentation.controllers.platform;

import com.washflow.domain.errors.EmailAlreadyRegisteredError;
import com.washflow.domain.errors.EstablishmentNotFoundError;
import com.washflow.domain.errors.EstablishmentSlugTakenError;
import com.washflow.domain.errors.InvalidCredentialsError;
import com.washflow.domain.usecases.ManagePlatform;
import com.washflow.presentation.controllers.establishment.BodyValues;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import java.util.Map;
import java.util.UUID;

/** Platform administrator endpoints and the public lava-jato directory. */
public final class PlatformController {

  private PlatformController() {}

  /** {@code POST /api/auth/admins}: returns the admin, turned into a session by the adapter. */
  public static Controller signIn(ManagePlatform platform) {
    return request -> {
      try {
        Map<String, Object> body = body(request.body());
        return HttpHelper.ok(
            platform.signIn(BodyValues.text(body, "email"), BodyValues.text(body, "password")));
      } catch (InvalidCredentialsError e) {
        return HttpHelper.unauthorized(e);
      } catch (Exception e) {
        return HttpHelper.serverError(e);
      }
    };
  }

  public static Controller list(ManagePlatform platform) {
    return request -> {
      try {
        return HttpHelper.ok(platform.list());
      } catch (Exception e) {
        return HttpHelper.serverError(e);
      }
    };
  }

  public static Controller create(ManagePlatform platform) {
    return request -> {
      try {
        Map<String, Object> body = body(request.body());
        return HttpHelper.ok(
            platform.create(
                new ManagePlatform.NewEstablishment(
                    BodyValues.text(body, "name"),
                    BodyValues.text(body, "slug"),
                    BodyValues.integer(body, "openingHour"),
                    BodyValues.integer(body, "closingHour"),
                    Boolean.TRUE.equals(body.get("openOnSunday")),
                    BodyValues.integer(body, "maxVehiclesPerHour"),
                    BodyValues.text(body, "managerName"),
                    BodyValues.text(body, "managerEmail"),
                    BodyValues.text(body, "managerPhone"),
                    BodyValues.text(body, "managerPassword"))));
      } catch (IllegalArgumentException e) {
        return HttpHelper.badRequest(e);
      } catch (EstablishmentSlugTakenError | EmailAlreadyRegisteredError e) {
        return HttpHelper.conflict(e);
      } catch (Exception e) {
        return HttpHelper.serverError(e);
      }
    };
  }

  public static Controller setActive(ManagePlatform platform) {
    return request -> {
      try {
        Object active = body(request.body()).get("active");
        if (!(active instanceof Boolean flag)) {
          throw new IllegalArgumentException("Informe active true ou false");
        }
        UUID id = UUID.fromString(request.pathParams().get("id"));
        return HttpHelper.ok(platform.setActive(id, flag));
      } catch (IllegalArgumentException e) {
        return HttpHelper.badRequest(e);
      } catch (EstablishmentNotFoundError e) {
        return HttpHelper.notFound(e);
      } catch (Exception e) {
        return HttpHelper.serverError(e);
      }
    };
  }

  /** {@code GET /api/establishments?query=}: public picker on the home screen. */
  public static Controller search(ManagePlatform platform) {
    return request -> {
      try {
        Map<String, String> query =
            request.queryParams() == null ? Map.of() : request.queryParams();
        return HttpHelper.ok(platform.search(query.get("query")));
      } catch (Exception e) {
        return HttpHelper.serverError(e);
      }
    };
  }

  private static Map<String, Object> body(Map<String, Object> body) {
    return body == null ? Map.of() : body;
  }
}
