package com.washflow.presentation.controllers.deletephoto;

import com.washflow.domain.usecases.DeleteServiceOrderPhoto;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.UUID;

public class DeletePhotoController implements Controller {

  private final DeleteServiceOrderPhoto deleteServiceOrderPhoto;

  public DeletePhotoController(DeleteServiceOrderPhoto deleteServiceOrderPhoto) {
    this.deleteServiceOrderPhoto = deleteServiceOrderPhoto;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      UUID photoId = UUID.fromString(request.pathParams().get("photoId"));
      deleteServiceOrderPhoto.delete(photoId);
      return new HttpResponse(204, "");
    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
