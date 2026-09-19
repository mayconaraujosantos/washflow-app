/**
 * Adapts Javalin's {@code io.javalin.http.Context} to the framework-agnostic
 * {@code presentation.protocols} (HttpRequest/HttpResponse), so controllers
 * never import Javalin directly.
 */
package com.washflow.infra.http;
