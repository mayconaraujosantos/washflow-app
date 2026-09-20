package com.washflow.presentation.controllers.hello;

import com.washflow.presentation.helpers.HttpHelper;
import com.washflow.presentation.protocols.Controller;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.Map;

public class HelloController implements Controller {

  @Override
  public HttpResponse handle(HttpRequest request) {
    return HttpHelper.ok(Map.of("message", "Hello from Javalin + React PWA"));
  }
}
