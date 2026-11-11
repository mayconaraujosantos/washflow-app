package com.washflow.domain.entities.vehicle;

import com.washflow.domain.errors.vehicle.InvalidPlateError;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * {@code veiculos} from CLAUDE.md. {@code customerId} ({@code cliente_id}) is nullable - clients
 * without a full signup. For those, {@code contactName}/{@code contactPhone} keep who to call when
 * the car is ready (filled when the manager registers a walk-in entry).
 */
public record Vehicle(
    UUID id,
    UUID customerId,
    String plate,
    String model,
    String color,
    String contactName,
    String contactPhone) {

  public static final int MAX_MODEL = 50;
  public static final int MAX_COLOR = 30;
  private static final Pattern PLATE = Pattern.compile("^[A-Z]{3}\\d[A-Z0-9]\\d{2}$");

  public Vehicle(UUID id, UUID customerId, String plate, String model, String color) {
    this(id, customerId, plate, model, color, null, null);
  }

  /**
   * Upper-case letters and digits only ({@code abc-1234} becomes {@code ABC1234}), so the plate the
   * customer types matches the one the manager entered at the counter. Old ({@code ABC1234}) and
   * Mercosul ({@code ABC1D23}) formats.
   */
  public static String normalizePlate(String raw) throws InvalidPlateError {
    String plate = raw == null ? "" : raw.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    if (!PLATE.matcher(plate).matches()) {
      throw new InvalidPlateError(raw == null ? "" : raw);
    }
    return plate;
  }
}
