package com.pilot.testharness.harness.scenario.assertion;

import com.pilot.testharness.harness.scenario.databse.TestDatabaseManager;
import com.pilot.testharness.harness.scenario.enums.HarnessType;
import com.pilot.testharness.harness.scenario.model.ScenarioFile;
import com.pilot.testharness.harness.scenario.utils.JsonNodes;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class DatabaseAssertion {

    private final TestDatabaseManager database;
    private final ObjectMapper objectMapper;

    public void assertScenario(List<ScenarioFile> files) {
        files.stream()
                .filter(file -> file.fileType() == HarnessType.DB_EXPECTED)
                .forEach(this::assertDatabase);
    }

    private void assertDatabase(ScenarioFile file) {
        JsonNode expected = file.jsonNode().path("dbExpected");
        String query = expected.path("query").asString("");

        JsonNode expectedRows = expected.path("rows").deepCopy();
        JsonNode actualRows = objectMapper.valueToTree(database.query(query));

        // Entries are column names (applied to every row) or JSON pointers such as /0/created_at.
        expected.path("ignoreFields").forEach(field -> {
            String name = field.asString();
            String pointer = name.startsWith("/") ? name : "/*/" + name.replace("~", "~0").replace("/", "~1");
            JsonNodes.remove(expectedRows, pointer);
            JsonNodes.remove(actualRows, pointer);
        });

        assertThat(JsonNodes.toPlainObject(objectMapper, actualRows))
                .as("Database rows for query [%s] in %s", query, file.fileName())
                .usingRecursiveComparison()
                .isEqualTo(JsonNodes.toPlainObject(objectMapper, expectedRows));
    }
}