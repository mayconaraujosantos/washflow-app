/**
 * Business errors, one subpackage per aggregate. Where an error goes:
 *
 * <ul>
 *   <li>{@code common}: only the categories ({@link com.washflow.domain.errors.common.DomainError}
 *       and its subclasses, which pick the HTTP status) and errors with no owning aggregate
 *       (generic access denied/unauthorized, missing tenant);
 *   <li>{@code auth}: the verification codes and Google sign-in, shared by {@code customer} and
 *       {@code user};
 *   <li>everything else: the aggregate that owns the entity or rule, even when other aggregates
 *       throw it ({@code ServicePriceNotFoundError} is {@code pricing}).
 * </ul>
 *
 * <p>Technical failures are not here: they stay in the layer that produces them, next to its port
 * ({@code DuplicateKeyError} and {@code EmailSendError} in {@code data/protocols}, {@code
 * ServerError} and {@code MissingParamError} in {@code presentation/errors}) and use cases turn
 * them into a domain error when the client must see them.
 */
package com.washflow.domain.errors;
