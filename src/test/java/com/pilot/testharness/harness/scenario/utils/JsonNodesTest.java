package com.pilot.testharness.harness.scenario.utils;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonNodesTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void removesNestedField() {
        JsonNode node = mapper.readTree("{\"a\":{\"b\":1,\"c\":2}}");

        JsonNodes.remove(node, "/a/b");

        assertThat(node.toString()).isEqualTo("{\"a\":{\"c\":2}}");
    }

    @Test
    void wildcardRemovesFieldFromEveryArrayElement() {
        JsonNode node = mapper.readTree("{\"users\":[{\"id\":1,\"ts\":\"x\"},{\"id\":2,\"ts\":\"y\"}]}");

        JsonNodes.remove(node, "/users/*/ts");

        assertThat(node.toString()).isEqualTo("{\"users\":[{\"id\":1},{\"id\":2}]}");
    }

    @Test
    void missingPathIsIgnored() {
        JsonNode node = mapper.readTree("{\"a\":1}");

        JsonNodes.remove(node, "/x/y/z");

        assertThat(node.toString()).isEqualTo("{\"a\":1}");
    }

    @Test
    void pointerMustStartWithSlash() {
        JsonNode node = mapper.readTree("{}");

        assertThatThrownBy(() -> JsonNodes.remove(node, "a"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
