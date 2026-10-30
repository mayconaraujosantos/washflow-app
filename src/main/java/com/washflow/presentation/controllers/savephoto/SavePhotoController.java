package com.washflow.presentation.controllers.savephoto;

import com.washflow.domain.entities.ServiceOrderPhoto.PhotoType;
import com.washflow.domain.errors.InvalidPhotoTypeError;
import com.washflow.domain.usecases.SaveServiceOrderPhoto;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.Map;
import java.util.UUID;

public class SavePhotoController implements Controller {

  private final SaveServiceOrderPhoto saveServiceOrderPhoto;

  public SavePhotoController(SaveServiceOrderPhoto saveServiceOrderPhoto) {
    this.saveServiceOrderPhoto = saveServiceOrderPhoto;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      UUID serviceOrderId = UUID.fromString(request.pathParams().get("id"));

      Map<String, Object> body = request.body();

      String typeStr = (String) body.get("type");
      if (typeStr == null || typeStr.isBlank()) {
        return HttpHelper.badRequest(new IllegalArgumentException("type is required"));
      }
      PhotoType type = PhotoType.valueOf(typeStr);

      String imageData = (String) body.get("imageData");
      if (imageData == null || imageData.isBlank()) {
        return HttpHelper.badRequest(new IllegalArgumentException("imageData is required"));
      }

      String description = (String) body.get("description");
      UUID createdBy = (UUID) body.get("createdBy");
      if (createdBy == null) {
        return HttpHelper.badRequest(new IllegalArgumentException("createdBy is required"));
      }

      SaveServiceOrderPhoto.Params params =
          new SaveServiceOrderPhoto.Params(serviceOrderId, type, imageData, description, createdBy);

      var saved = saveServiceOrderPhoto.save(params);
      return new HttpResponse(201, saved);

    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (InvalidPhotoTypeError e) {
      return HttpHelper.badRequest(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
