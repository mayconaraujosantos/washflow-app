package com.washflow.presentation.protocols;

import java.util.Map;

/** Framework-agnostic inbound request. {@code JavalinRouteAdapter} builds this from a Context. */
public record HttpRequest(Map<String, Object> body, Map<String, String> pathParams) {}
