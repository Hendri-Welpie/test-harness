package com.pilot.testharness.harness.scenario;

import com.pilot.testharness.TestHarnessApplication;
import com.pilot.testharness.harness.scenario.databse.TestDatabase;
import com.pilot.testharness.harness.scenario.databse.TestDatabaseManager;
import com.pilot.testharness.harness.scenario.executor.ScenarioExecutor;
import com.pilot.testharness.harness.scenario.model.Scenario;
import com.pilot.testharness.harness.scenario.utils.ScenarioScanner;
import com.pilot.testharness.harness.scenario.utils.ScenarioValidator;
import com.pilot.testharness.harness.scenario.wiremock.DownstreamMockServer;
import com.pilot.testharness.harness.scenario.wiremock.WireMockHolder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import tools.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.util.stream.Stream;

@SpringBootTest(
        classes = TestHarnessApplication.class,
        webEnvironment =
                SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@Import(TestDatabase.class)
class IntegrationHarnessTest {

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    DataSource dataSource;

    @LocalServerPort
    int applicationPort;

    @DynamicPropertySource
    static void registerDynamicProperties(
            DynamicPropertyRegistry registry
    ) {

        registry.add(
                "harness.downstream-url",
                WireMockHolder.SERVER::baseUrl
        );
    }

    @AfterAll
    static void shutdownWireMock() {

        if (WireMockHolder.SERVER.isRunning()) {
            WireMockHolder.SERVER.stop();
        }
    }

    @ParameterizedTest(
            name = "Scenario: {0}"
    )
    @MethodSource("scenarios")
    void executeScenario(
            String scenarioName,
            Scenario scenario
    ) throws Exception {

        new ScenarioValidator()
                .validate(scenario);

        TestDatabaseManager database =
                new TestDatabaseManager(
                        dataSource
                );

        DownstreamMockServer downstream =
                new DownstreamMockServer(
                        WireMockHolder.SERVER,
                        objectMapper
                );

        ScenarioExecutor executor =
                new ScenarioExecutor(
                        objectMapper,
                        downstream,
                        database
                );

        executor.execute(
                scenario,
                "http://localhost:"
                        + applicationPort
        );
    }

    static Stream<Arguments> scenarios() {

        ObjectMapper mapper =
                new ObjectMapper();

        ScenarioScanner discovery =
                new ScenarioScanner(mapper);

        return discovery
                .discoverScenarios()
                .stream()
                .map(scenario ->
                        Arguments.of(
                                scenario.name(),
                                scenario
                        )
                );
    }
}