package com.washflow.domain.entities.whatsapp;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * What the customer chose so far, plus the options last offered: "2" means the second item the
 * customer saw, even if the catalog or the free hours changed since. Stored as JSON with the
 * conversation, so it keeps plain getters and setters.
 */
public class ConversationDraft {

  private UUID vehicleId;
  private String plate;
  private List<Integer> serviceIds = new ArrayList<>();
  private LocalDate day;
  private Instant slot;

  /** A time the customer asked for in free text, matched against the free hours of the day. */
  private LocalTime time;

  /** "MORNING" or "AFTERNOON" when the customer asked for one (free text); narrows the hours. */
  private String period;

  private List<UUID> vehicleOptions = new ArrayList<>();
  private List<Integer> serviceOptions = new ArrayList<>();
  private List<LocalDate> dayOptions = new ArrayList<>();
  private List<Instant> slotOptions = new ArrayList<>();
  private List<UUID> orderOptions = new ArrayList<>();
  private UUID orderToCancel;
  private Instant humanUntil;

  public UUID getVehicleId() {
    return vehicleId;
  }

  public void setVehicleId(UUID vehicleId) {
    this.vehicleId = vehicleId;
  }

  public String getPlate() {
    return plate;
  }

  public void setPlate(String plate) {
    this.plate = plate;
  }

  public List<Integer> getServiceIds() {
    return serviceIds;
  }

  public void setServiceIds(List<Integer> serviceIds) {
    this.serviceIds = serviceIds == null ? new ArrayList<>() : new ArrayList<>(serviceIds);
  }

  public LocalDate getDay() {
    return day;
  }

  public void setDay(LocalDate day) {
    this.day = day;
  }

  public Instant getSlot() {
    return slot;
  }

  public void setSlot(Instant slot) {
    this.slot = slot;
  }

  public LocalTime getTime() {
    return time;
  }

  public void setTime(LocalTime time) {
    this.time = time;
  }

  public List<Integer> getServiceOptions() {
    return serviceOptions;
  }

  public void setServiceOptions(List<Integer> serviceOptions) {
    this.serviceOptions =
        serviceOptions == null ? new ArrayList<>() : new ArrayList<>(serviceOptions);
  }

  public String getPeriod() {
    return period;
  }

  public void setPeriod(String period) {
    this.period = period;
  }

  public List<UUID> getVehicleOptions() {
    return vehicleOptions;
  }

  public void setVehicleOptions(List<UUID> vehicleOptions) {
    this.vehicleOptions =
        vehicleOptions == null ? new ArrayList<>() : new ArrayList<>(vehicleOptions);
  }

  public List<LocalDate> getDayOptions() {
    return dayOptions;
  }

  public void setDayOptions(List<LocalDate> dayOptions) {
    this.dayOptions = dayOptions == null ? new ArrayList<>() : new ArrayList<>(dayOptions);
  }

  public List<Instant> getSlotOptions() {
    return slotOptions;
  }

  public void setSlotOptions(List<Instant> slotOptions) {
    this.slotOptions = slotOptions == null ? new ArrayList<>() : new ArrayList<>(slotOptions);
  }

  public List<UUID> getOrderOptions() {
    return orderOptions;
  }

  public void setOrderOptions(List<UUID> orderOptions) {
    this.orderOptions = orderOptions == null ? new ArrayList<>() : new ArrayList<>(orderOptions);
  }

  public UUID getOrderToCancel() {
    return orderToCancel;
  }

  public void setOrderToCancel(UUID orderToCancel) {
    this.orderToCancel = orderToCancel;
  }

  public Instant getHumanUntil() {
    return humanUntil;
  }

  public void setHumanUntil(Instant humanUntil) {
    this.humanUntil = humanUntil;
  }
}
