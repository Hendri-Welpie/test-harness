package com.pilot.testharness.harness.scenario.utils;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.Arrays;
import java.util.List;

public final class JsonNodes {

    private JsonNodes() {
    }

    /**
     * Removes every node matching the JSON pointer. A {@code *} segment matches every
     * element of an array (or every property of an object), e.g. {@code /users/*}{@code /createdAt}.
     */
    public static void remove(JsonNode root, String pointer) {
        if (root == null || pointer == null || pointer.isBlank()) {
            return;
        }
        if (!pointer.startsWith("/")) {
            throw new IllegalArgumentException("JSON pointer must start with '/': " + pointer);
        }

        List<String> segments = Arrays.stream(pointer.substring(1).split("/", -1))
                .map(segment -> segment.replace("~1", "/").replace("~0", "~"))
                .toList();

        removeFrom(root, segments, 0);
    }

    public static void removeAll(JsonNode root, JsonNode pointers) {
        if (pointers != null && pointers.isArray()) {
            pointers.forEach(pointer -> remove(root, pointer.asString()));
        }
    }

    /**
     * Converts a tree into plain Java objects so numeric types are normalised
     * (an int column and a JSON integer compare equal).
     */
    public static Object toPlainObject(ObjectMapper objectMapper, JsonNode node) {
        return objectMapper.readValue(node.toString(), Object.class);
    }

    private static void removeFrom(JsonNode node, List<String> segments, int index) {
        if (node == null) {
            return;
        }

        String segment = segments.get(index);

        if (index == segments.size() - 1) {
            if (node instanceof ObjectNode objectNode) {
                objectNode.remove(segment);
            } else if (node instanceof ArrayNode arrayNode && isIndex(segment)) {
                int position = Integer.parseInt(segment);
                if (position < arrayNode.size()) {
                    arrayNode.remove(position);
                }
            }
            return;
        }

        if ("*".equals(segment)) {
            for (JsonNode child : node) {
                removeFrom(child, segments, index + 1);
            }
            return;
        }

        if (node.isObject()) {
            removeFrom(node.get(segment), segments, index + 1);
        } else if (node.isArray() && isIndex(segment)) {
            removeFrom(node.get(Integer.parseInt(segment)), segments, index + 1);
        }
    }

    private static boolean isIndex(String segment) {
        return !segment.isEmpty() && segment.chars().allMatch(Character::isDigit);
    }
}
