package com.washflow.presentation.controllers.serviceorder;

import com.washflow.domain.entities.PaymentMethod;

public record FinalizeServiceOrderRequest(PaymentMethod paymentMethod) {}
