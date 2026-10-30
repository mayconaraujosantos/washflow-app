package com.washflow.presentation.protocols;

/**
 * One per route/use case. Implementations must never let an exception escape {@link
 * #handle(HttpRequest)} - catch everything and translate it into an {@code HttpResponse} (see
 * {@code presentation.helpers.HttpHelper}), the same discipline clean-ts-api's controllers follow.
 */
public interface Controller {

  HttpResponse handle(HttpRequest request);
}
