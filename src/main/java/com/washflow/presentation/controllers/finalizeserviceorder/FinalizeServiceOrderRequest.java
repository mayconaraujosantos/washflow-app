package com.washflow.presentation.controllers.finalizeserviceorder;

import com.washflow.domain.entities.PaymentMethod;

public record FinalizeServiceOrderRequest(PaymentMethod paymentMethod) {}
