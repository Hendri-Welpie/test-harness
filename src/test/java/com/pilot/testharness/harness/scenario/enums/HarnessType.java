package com.pilot.testharness.harness.scenario.enums;

import lombok.Getter;

@Getter
public enum HarnessType {
    HTTP_MOCK,
    HTTP_REQUEST,
    HTTP_EXPECTED_RESPONSE,
    DB_EXPECTED
}
