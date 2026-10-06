package com.pilot.testharness.user;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record User(Long id, String name, String email, Instant createdAt) {
}