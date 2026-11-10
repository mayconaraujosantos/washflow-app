package com.washflow.presentation.controllers.serviceorder;

import com.washflow.domain.entities.shared.PaymentMethod;

public record FinalizeServiceOrderRequest(PaymentMethod paymentMethod) {}
