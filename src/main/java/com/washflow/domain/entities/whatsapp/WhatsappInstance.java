package com.washflow.domain.entities.whatsapp;

import java.util.Optional;
import java.util.UUID;

/**
 * Each lava-jato links its own WhatsApp number as one Evolution API instance, named after the
 * establishment id: the name never changes (the slug can) and a message received by the instance
 * says which lava-jato it belongs to.
 */
public final class WhatsappInstance {

  private static final String PREFIX = "washflow-";

  private WhatsappInstance() {}

  public static String nameOf(UUID establishmentId) {
    return PREFIX + establishmentId;
  }

  /** The lava-jato of an instance name; empty for any other instance (the login-code number). */
  public static Optional<UUID> establishmentOf(String instanceName) {
    if (instanceName == null || !instanceName.startsWith(PREFIX)) {
      return Optional.empty();
    }
    try {
      return Optional.of(UUID.fromString(instanceName.substring(PREFIX.length())));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }
}
