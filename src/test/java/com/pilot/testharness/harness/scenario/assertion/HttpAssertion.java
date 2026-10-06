package com.pilot.testharness.harness.scenario.assertion;

import com.pilot.testharness.harness.scenario.enums.HarnessType;
import com.pilot.testharness.harness.scenario.model.ScenarioFile;
import com.pilot.testharness.harness.scenario.utils.JsonNodes;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class HttpAssertion {

    private final ObjectMapper objectMapper;

    public void assertScenario(List<ScenarioFile> files, HttpResponse<String> actual) {
        files.stream()
                .filter(file -> file.fileType() == HarnessType.HTTP_EXPECTED_RESPONSE)
                .forEach(file -> assertHttp(file, actual));
    }

    private void assertHttp(ScenarioFile file, HttpResponse<String> actual) {
        JsonNode expected = file.jsonNode().path("httpExpected");

        assertThat(actual.statusCode())
                .as("HTTP status [%s]", file.fileName())
                .isEqualTo(expected.path("statusCode").asInt(200));

        expected.path("headers").propertyStream().forEach(header ->
                assertThat(actual.headers().firstValue(header.getKey()))
                        .as("HTTP header '%s' [%s]", header.getKey(), file.fileName())
                        .hasValueSatisfying(value ->
                                assertThat(value).startsWithIgnoringCase(header.getValue().asString())));

        if (!expected.hasNonNull("body")) {
            return;
        }

        assertThat(actual.body())
                .as("HTTP response body [%s]", file.fileName())
                .isNotBlank();

        JsonNode expectedBody = expected.get("body").deepCopy();
        JsonNode actualBody = objectMapper.readTree(actual.body());

        // Dynamic fields (ids, timestamps) are removed from both sides before comparing.
        JsonNodes.removeAll(expectedBody, expected.path("ignoreFields"));
        JsonNodes.removeAll(actualBody, expected.path("ignoreFields"));

        assertThat(JsonNodes.toPlainObject(objectMapper, actualBody))
                .as("HTTP response body [%s]", file.fileName())
                .usingRecursiveComparison()
                .isEqualTo(JsonNodes.toPlainObject(objectMapper, expectedBody));
    }
}