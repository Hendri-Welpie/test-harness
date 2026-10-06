package com.pilot.testharness.harness.scenario.model;

import org.springframework.core.io.Resource;

import java.util.List;

public record Scenario(String name, Resource directory, List<ScenarioFile> files) {
}
