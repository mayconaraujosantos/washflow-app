package com.washflow.application.routes.requests;

public record AuthenticateCredentialsRequest(String phone, String password) {}
