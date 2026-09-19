package com.washflow.presentation.protocols;

/** Framework-agnostic outbound response. {@code JavalinRouteAdapter} writes this onto a Context. */
public record HttpResponse(int statusCode, Object body) {}
