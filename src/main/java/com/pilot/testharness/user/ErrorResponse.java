package com.pilot.testharness.user;

import java.time.Instant;

public record ErrorResponse(String error, Instant timestamp) {

    public static ErrorResponse of(String error) {
        return new ErrorResponse(error, Instant.now());
    }
}