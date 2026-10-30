package com.washflow.presentation.controllers.getphotos;

import com.washflow.domain.entities.ServiceOrderPhoto;
import com.washflow.domain.usecases.ListServiceOrderPhotos;
import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.List;
import java.util.UUID;

public class GetPhotosController implements Controller {

  private final ListServiceOrderPhotos listServiceOrderPhotos;

  public GetPhotosController(ListServiceOrderPhotos listServiceOrderPhotos) {
    this.listServiceOrderPhotos = listServiceOrderPhotos;
  }

  @Override
  public HttpResponse handle(HttpRequest request) {
    try {
      UUID serviceOrderId = UUID.fromString(request.pathParams().get("id"));
      List<ServiceOrderPhoto> photos = listServiceOrderPhotos.list(serviceOrderId);
      return HttpHelper.ok(photos);
    } catch (IllegalArgumentException e) {
      return HttpHelper.badRequest(e);
    } catch (Exception e) {
      return HttpHelper.serverError(e);
    }
  }
}
