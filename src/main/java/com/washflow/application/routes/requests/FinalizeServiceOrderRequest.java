package com.washflow.application.routes.requests;

import com.washflow.domain.entities.shared.PaymentMethod;

public record FinalizeServiceOrderRequest(PaymentMethod paymentMethod) {}
