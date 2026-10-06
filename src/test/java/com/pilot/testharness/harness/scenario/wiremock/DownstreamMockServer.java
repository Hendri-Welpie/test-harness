package com.pilot.testharness.harness.scenario.wiremock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.pilot.testharness.harness.scenario.enums.HarnessType;
import com.pilot.testharness.harness.scenario.model.Scenario;
import com.pilot.testharness.harness.scenario.model.ScenarioFile;
import com.pilot.testharness.harness.scenario.utils.JsonNodes;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class DownstreamMockServer {

    private final WireMockServer server;
    private final ObjectMapper objectMapper;

    public DownstreamMockServer(
            WireMockServer server,
            ObjectMapper objectMapper
    ) {
        this.server = server;
        this.objectMapper = objectMapper;
    }

    public void configure(Scenario scenario) {
        server.resetAll();

        scenario.files()
                .stream()
                .filter(file -> file.fileType() == HarnessType.HTTP_MOCK)
                .forEach(this::registerMock);
    }

    private void registerMock(ScenarioFile file) {
        JsonNode root = file.jsonNode();

        JsonNode mock = root.path("mock");

        MappingBuilder mapping = buildRequest(mock.path("request"));
        ResponseDefinitionBuilder response = buildResponse(mock.path("response"));

        server.stubFor(mapping.willReturn(response));
    }

    private MappingBuilder buildRequest(JsonNode request) {
        String method = request.path("method").asString("").toUpperCase();
        String path =         request.path("url").asString("");

        if (method.isBlank() || path.isBlank()) {
                    throw new IllegalArgumentException("Missing required 'method' or 'url' fields in request mock spec.");
        }

        MappingBuilder mapping = switch (method) {
            case "GET" -> get(urlPathEqualTo(path));
            case "POST" -> post(urlPathEqualTo(path));
            case "PUT" -> put(urlPathEqualTo(path));
            case "PATCH" -> patch(urlPathEqualTo(path));
            case "DELETE" -> delete(urlPathEqualTo(path));
            case "HEAD" -> head(urlPathEqualTo(path));
            case "OPTIONS" -> options(urlPathEqualTo(path));
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        };

        if (request.hasNonNull("params")) {
            request.get("params")
                    .propertyStream()
                    .forEach(entry -> {
                        mapping.withQueryParam(entry.getKey(), equalTo(entry.getValue().asString()));
                    });
        }

        if (request.hasNonNull("headers")) {
            request.get("headers")
                    .propertyStream()
                    .forEach(entry -> {
                        mapping.withHeader(entry.getKey(), equalTo(entry.getValue().asString()));
                    });
        }

        if (request.hasNonNull("body")) {
            JsonNode expectedBody = request.get("body").deepCopy();
            JsonNodes.removeAll(expectedBody, request.path("ignoreFields"));

            // PARTIAL (default): extra fields and array order in the real request are tolerated.
            // Values may also use WireMock placeholders such as "${json-unit.ignore}".
            boolean exact = "EXACT".equalsIgnoreCase(request.path("bodyMatch").asString("PARTIAL"));

            mapping.withRequestBody(
                    equalToJson(
                            expectedBody.toString(),
                            !exact,
                            !exact
                    )
            );
        }

        return mapping;
    }

    private ResponseDefinitionBuilder buildResponse(JsonNode response) {
        int status = response.path("statusCode").asInt(200);
        var builder = aResponse().withStatus(status);

        if (response.hasNonNull("headers")) {
            response.get("headers")
                    .propertyStream()
                    .forEach(entry -> {
                        builder.withHeader(entry.getKey(), entry.getValue().asString());
                    });
        }

        if (response.hasNonNull("body")) {
            if (!response.path("headers").has("Content-Type")) {
                builder.withHeader("Content-Type", "application/json");
            }
            JsonNode body = response.get("body");
            try {
                builder.withBody(
                        body.isString()
                                ? body.asString()
                                : objectMapper.writeValueAsString(body)
                );
            } catch (Exception e) {
                throw new IllegalStateException("Unable to serialize mock response body text", e);
            }
        }

        if (response.hasNonNull("fixedDelayMs")) {
            builder.withFixedDelay(response.get("fixedDelayMs").asInt());
        }

        return builder;
    }

    public void assertNoUnexpectedRequests() {
        var unmatched = server.findAllUnmatchedRequests();
        if (unmatched.isEmpty()) {
            return;
        }

        StringBuilder message = new StringBuilder("Unexpected downstream requests detected:\n");
        unmatched.forEach(request ->
                message.append("- ")
                        .append(request.getMethod())
                        .append(" ")
                        .append(request.getUrl())
                        .append("\n")
        );

        throw new AssertionError(message.toString());
    }
}