package com.pilot.testharness.harness.scenario.executor;

import com.pilot.testharness.harness.scenario.assertion.DatabaseAssertion;
import com.pilot.testharness.harness.scenario.assertion.HttpAssertion;
import com.pilot.testharness.harness.scenario.databse.TestDatabaseManager;
import com.pilot.testharness.harness.scenario.enums.HarnessType;
import com.pilot.testharness.harness.scenario.model.Scenario;
import com.pilot.testharness.harness.scenario.model.ScenarioFile;
import com.pilot.testharness.harness.scenario.wiremock.DownstreamMockServer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ScenarioExecutor {

    private final ObjectMapper objectMapper;
    private final DownstreamMockServer downstream;
    private final TestDatabaseManager database;
    private final HttpAssertion httpAssertion;
    private final DatabaseAssertion databaseAssertion;
    private final HttpClient httpClient;

    public ScenarioExecutor(
            ObjectMapper objectMapper,
            DownstreamMockServer downstream,
            TestDatabaseManager database
    ) {
        this.objectMapper = objectMapper;
        this.downstream = downstream;
        this.database = database;
        this.httpAssertion = new HttpAssertion(objectMapper);
        this.databaseAssertion = new DatabaseAssertion(database, objectMapper);
        this.httpClient = HttpClient.newHttpClient();
    }

    public void execute(
            Scenario scenario,
            String applicationBaseUrl
    ) throws Exception {

        /*
         * 1. Completely isolate scenario.
         */
        database.reset();

        downstream.configure(
                scenario
        );

        /*
         * 2. Find the one request.
         */
        ScenarioFile request =
                scenario.files()
                        .stream()
                        .filter(file ->
                                file.fileType() ==
                                        HarnessType.HTTP_REQUEST)
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Scenario has no REQUEST: "
                                                        + scenario.name()
                                        )
                        );

        /*
         * 3. Execute against the REAL application.
         */
        HttpResponse<String> response =
                executeRequest(
                        request,
                        applicationBaseUrl
                );

        /*
         * 4. Verify HTTP result.
         *
         * If no HTTP_EXPECTATION exists,
         * nothing is asserted here.
         */
        httpAssertion.assertScenario(
                scenario.files(),
                response
        );

        /*
         * 5. Verify DB result.
         *
         * If no DB_EXPECTATION exists,
         * nothing is asserted here.
         */
        databaseAssertion.assertScenario(
                scenario.files()
        );

        /*
         * 6. Make sure application didn't call
         *    an unexpected downstream.
         */
        downstream.assertNoUnexpectedRequests();
    }

    private HttpResponse<String> executeRequest(
            ScenarioFile file,
            String applicationBaseUrl
    ) throws Exception {

        JsonNode json =
                file.jsonNode()
                        .get("httpRequest");

        String method =
                json.get("method")
                        .asText()
                        .toUpperCase();

        String path =
                json.get("url")
                        .asText();

        StringBuilder url =
                new StringBuilder(
                        applicationBaseUrl
                ).append(path);

        if (json.has("params")) {

            boolean first = !path.contains("?");

            var fields =
                    json.get("params")
                            .propertyStream().iterator();

            while (fields.hasNext()) {

                var entry =
                        fields.next();

                url.append(
                        first ? '?' : '&'
                );

                first = false;

                url.append(
                        java.net.URLEncoder.encode(
                                entry.getKey(),
                                java.nio.charset.StandardCharsets.UTF_8
                        )
                );

                url.append('=');

                url.append(
                        java.net.URLEncoder.encode(
                                entry.getValue().asText(),
                                java.nio.charset.StandardCharsets.UTF_8
                        )
                );
            }
        }

        HttpRequest.Builder builder =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        url.toString()
                                )
                        );

        if (json.has("headers")) {

            json.get("headers")
                    .propertyStream().iterator()
                    .forEachRemaining(entry ->
                            builder.header(
                                    entry.getKey(),
                                    entry.getValue()
                                            .asText()
                            )
                    );
        }

        if (json.has("body")) {

            String body =
                    objectMapper.writeValueAsString(
                            json.get("body")
                    );

            builder.method(
                    method,
                    HttpRequest.BodyPublishers
                            .ofString(body)
            );

        } else {

            builder.method(
                    method,
                    HttpRequest.BodyPublishers
                            .noBody()
            );
        }

        return httpClient.send(
                builder.build(),
                HttpResponse.BodyHandlers
                        .ofString()
        );
    }
}