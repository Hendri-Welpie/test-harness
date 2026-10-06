package com.pilot.testharness.harness.scenario.model;


import com.pilot.testharness.harness.scenario.enums.HarnessType;
import org.springframework.core.io.Resource;
import tools.jackson.databind.JsonNode;

public record ScenarioFile(Resource path, String fileName, HarnessType fileType, JsonNode jsonNode) {
}
