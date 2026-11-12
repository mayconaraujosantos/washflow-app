package com.washflow.presentation.controllers.serviceorder;

import com.washflow.domain.entities.serviceorder.ServiceOrderPhoto.PhotoType;
import com.washflow.domain.usecases.serviceorder.SaveServiceOrderPhoto;
import com.washflow.presentation.helpers.BodyValues;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.Map;
import java.util.UUID;

public class SaveServiceOrderPhotoController implements Controller {

  private final SaveServiceOrderPhoto saveServiceOrderPhoto;

  public SaveServiceOrderPhotoController(SaveServiceOrderPhoto saveServiceOrderPhoto) {
    this.saveServiceOrderPhoto = saveServiceOrderPhoto;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      UUID serviceOrderId = UUID.fromString(request.pathParams().get("id"));

      Map<String, Object> body = request.body();

      String typeStr = BodyValues.text(body, "type");
      if (typeStr == null || typeStr.isBlank()) {
        return HttpHelper.badRequest(new IllegalArgumentException("type is required"));
      }
      PhotoType type = PhotoType.valueOf(typeStr);

      String imageData = BodyValues.text(body, "imageData");
      if (imageData == null || imageData.isBlank()) {
        return HttpHelper.badRequest(new IllegalArgumentException("imageData is required"));
      }

      String description = BodyValues.text(body, "description");
      UUID createdBy = BodyValues.createdBy(body);
      if (createdBy == null) {
        return HttpHelper.badRequest(new IllegalArgumentException("createdBy is required"));
      }

      SaveServiceOrderPhoto.Params params =
          new SaveServiceOrderPhoto.Params(serviceOrderId, type, imageData, description, createdBy);

      var saved = saveServiceOrderPhoto.save(params);
      return new HttpResponse(201, saved);

    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
