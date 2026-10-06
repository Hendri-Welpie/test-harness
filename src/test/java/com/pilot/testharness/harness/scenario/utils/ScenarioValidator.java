package com.pilot.testharness.harness.scenario.utils;

import com.pilot.testharness.harness.scenario.enums.HarnessType;
import com.pilot.testharness.harness.scenario.model.Scenario;
import com.pilot.testharness.harness.scenario.model.ScenarioFile;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Slf4j
public class ScenarioValidator {

    private static final Predicate<JsonNode> IS_CONTAINER = node -> node.isObject() || node.isArray();

    private static final Predicate<JsonNode> IS_STRING_ARRAY = node -> {
        if (!node.isArray()) {
            return false;
        }
        for (JsonNode element : node) {
            if (!element.isString()) {
                return false;
            }
        }
        return true;
    };

    // Optional by default, but strictly type-checked if present
    private static final Map<String, Predicate<JsonNode>> COMMON_HTTP_FIELDS = Map.of(
            "headers", JsonNode::isObject,
            "body", IS_CONTAINER
    );

    // Outbound HTTP requests: httpRequest and mock.request
    private static final Map<String, Predicate<JsonNode>> HTTP_REQUEST_SCHEMA = mergeSchemas(Map.of(
            "method", JsonNode::isString,
            "url", JsonNode::isString,
            "params", JsonNode::isObject,
            "bodyMatch", JsonNode::isString,
            "ignoreFields", IS_STRING_ARRAY
    ));

    // HTTP responses: httpExpected and mock.response
    private static final Map<String, Predicate<JsonNode>> HTTP_RESPONSE_SCHEMA = mergeSchemas(Map.of(
            "statusCode", JsonNode::isInt,
            "fixedDelayMs", JsonNode::isInt,
            "ignoreFields", IS_STRING_ARRAY
    ));

    public void validate(final Scenario scenario) {
        log.info("Validating scenario {}", scenario.name());
        validateNotNullOrEmpty(scenario.name());
        if (scenario.directory() == null) {
            log.error("Scenario {} has no directory", scenario.name());
            throw new IllegalArgumentException("Scenario directory cannot be null");
        }
        if (scenario.files() == null || scenario.files().isEmpty()) {
            log.error("Scenario {} has no files in directory {}", scenario.name(), scenario.directory().getFilename());
            throw new IllegalArgumentException("Scenario files cannot be null or empty");
        }

        List<ScenarioFile> files = scenario.files();

        files.stream()
                .filter(file -> file.fileType() == null)
                .findFirst()
                .ifPresent(file -> fail(scenario, file, "is missing a valid 'type' property"));

        long requestCount = files.stream()
                .filter(file -> file.fileType() == HarnessType.HTTP_REQUEST)
                .count();

        if (requestCount == 0) {
            log.error("Scenario {} does not have any request file", scenario.name());
            throw new IllegalStateException(String.format("Scenario %s does not have any request file", scenario.name()));
        }
        if (requestCount > 1) {
            log.error("Scenario {} has more than one request file", scenario.name());
            throw new IllegalStateException(String.format("Scenario %s has more than one request file", scenario.name()));
        }

        boolean hasOutcome = files.stream().anyMatch(file ->
                file.fileType() == HarnessType.HTTP_EXPECTED_RESPONSE ||
                        file.fileType() == HarnessType.DB_EXPECTED);

        if (!hasOutcome) {
            log.error("Scenario {} does not have any outcome file", scenario.name());
            throw new IllegalStateException(String.format("Scenario %s does not have any outcome file", scenario.name()));
        }

        validateMockFiles(scenario);
        validateRequestFiles(scenario);
        validateExpectedResponseFiles(scenario);
        validateExpectedDBFiles(scenario);
    }

    private void validateMockFiles(final Scenario scenario) {
        log.info("Validating mock files for scenario {}", scenario.name());
        forEachFile(scenario, HarnessType.HTTP_MOCK, file -> {
            JsonNode mock = requiredObject(file.jsonNode(), "mock", scenario, file);
            validateHttpNode(requiredObject(mock, "request", scenario, file), HTTP_REQUEST_SCHEMA,
                    List.of("method", "url"), scenario, file);
            validateHttpNode(requiredObject(mock, "response", scenario, file), HTTP_RESPONSE_SCHEMA,
                    List.of("statusCode"), scenario, file);
        });
    }

    private void validateRequestFiles(final Scenario scenario) {
        log.info("Validating request files for scenario {}", scenario.name());
        forEachFile(scenario, HarnessType.HTTP_REQUEST, file -> {
            JsonNode request = requiredObject(file.jsonNode(), "httpRequest", scenario, file);
            validateHttpNode(request, HTTP_REQUEST_SCHEMA, List.of("method", "url"), scenario, file);
        });
    }

    private void validateExpectedResponseFiles(final Scenario scenario) {
        log.info("Validating expected response files for scenario {}", scenario.name());
        forEachFile(scenario, HarnessType.HTTP_EXPECTED_RESPONSE, file -> {
            JsonNode expected = requiredObject(file.jsonNode(), "httpExpected", scenario, file);
            validateHttpNode(expected, HTTP_RESPONSE_SCHEMA, List.of("statusCode"), scenario, file);
        });
    }

    private void validateExpectedDBFiles(final Scenario scenario) {
        log.info("Validating expected DB files for scenario {}", scenario.name());
        forEachFile(scenario, HarnessType.DB_EXPECTED, file -> {
            JsonNode dbExpected = requiredObject(file.jsonNode(), "dbExpected", scenario, file);
            validateHttpNode(dbExpected, Map.of(
                    "query", JsonNode::isString,
                    "rows", JsonNode::isArray,
                    "ignoreFields", IS_STRING_ARRAY
            ), List.of("query", "rows"), scenario, file);
        });
    }

    private void forEachFile(Scenario scenario, HarnessType type, java.util.function.Consumer<ScenarioFile> action) {
        scenario.files().stream()
                .filter(file -> file.fileType() == type)
                .forEach(action);
    }

    private void validateHttpNode(
            JsonNode node,
            Map<String, Predicate<JsonNode>> schema,
            List<String> requiredFields,
            Scenario scenario,
            ScenarioFile file
    ) {
        requiredFields.forEach(field -> {
            if (!node.hasNonNull(field)) {
                fail(scenario, file, "is missing required field: " + field);
            }
        });

        schema.forEach((fieldName, typeCheck) -> {
            if (node.has(fieldName) && !typeCheck.test(node.get(fieldName))) {
                fail(scenario, file, "has an invalid " + fieldName + " field");
            }
        });
    }

    private JsonNode requiredObject(JsonNode parent, String field, Scenario scenario, ScenarioFile file) {
        if (parent == null || !parent.has(field) || !parent.get(field).isObject()) {
            fail(scenario, file, "is missing required object: " + field);
        }
        return parent.get(field);
    }

    private void fail(Scenario scenario, ScenarioFile file, String reason) {
        String message = String.format("File %s in scenario %s %s", file.fileName(), scenario.name(), reason);
        log.error(message);
        throw new IllegalStateException(message);
    }

    private void validateNotNullOrEmpty(String value) {
        if (value == null || value.isEmpty()) {
            log.error("Scenario name cannot be null or empty");
            throw new IllegalArgumentException("Scenario name cannot be null or empty");
        }
    }

    private static Map<String, Predicate<JsonNode>> mergeSchemas(Map<String, Predicate<JsonNode>> extensions) {
        Map<String, Predicate<JsonNode>> merged = new HashMap<>(COMMON_HTTP_FIELDS);
        merged.putAll(extensions);
        return Map.copyOf(merged);
    }
}
