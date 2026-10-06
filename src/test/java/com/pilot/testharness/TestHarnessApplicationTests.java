package com.pilot.testharness;

import com.pilot.testharness.harness.scenario.model.Scenario;
import com.pilot.testharness.harness.scenario.utils.ScenarioScanner;
import com.pilot.testharness.harness.scenario.utils.ScenarioValidator;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class TestHarnessApplicationTests {

//    @Test
//    void scenariosAreDiscoveredAndValid() {
//        List<Scenario> scenarios = new ScenarioScanner(JsonMapper.builder().build()).discoverScenarios();
//
//        assertThat(scenarios).extracting(Scenario::name)
//                .containsExactly(
//                        "scenario-get-user-not-found",
//                        "scenario-get-users-success",
//                        "scenario-post-user-success"
//                );
//
//        ScenarioValidator validator = new ScenarioValidator();
//        scenarios.forEach(scenario -> {
//            System.out.println("Scenario: " + scenario.name());
//            scenario.files().forEach(file ->
//                    System.out.println("  File: " + file.fileName() + ", Type: " + file.fileType()));
//            assertThatCode(() -> validator.validate(scenario)).doesNotThrowAnyException();
//        });
//    }
}
