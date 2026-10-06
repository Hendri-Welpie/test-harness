package com.pilot.testharness.harness.scenario.utils;

import com.pilot.testharness.harness.scenario.enums.HarnessType;
import com.pilot.testharness.harness.scenario.model.Scenario;
import com.pilot.testharness.harness.scenario.model.ScenarioFile;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ScenarioScanner {

    private static final String SCENARIOS_DIRECTORY = "scenarios";
    private final ObjectMapper objectMapper;

    public List<Scenario> discoverScenarios() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:" + SCENARIOS_DIRECTORY + "/**/*");

            Map<String, List<Resource>> groupedByScenario = new HashMap<>();
            Map<String, Resource> directoryMapping = new HashMap<>();

            // 1. Group the classpath files by their respective scenario folders
            for (Resource resource : resources) {
                if (!resource.exists()) continue;

                String urlStr = resource.getURL().toString();

                if (urlStr.endsWith("/")) {
                    String dirName = resource.getFilename();
                    if (dirName != null && !dirName.equals(SCENARIOS_DIRECTORY)) {
                        directoryMapping.put(dirName, resource);
                    }
                    continue;
                }

                String path = resource.getURL().getPath();
                String[] pathParts = path.split("/");
                if (pathParts.length > 1) {
                    String scenarioName = pathParts[pathParts.length - 2];

                    if (!scenarioName.equals(SCENARIOS_DIRECTORY)) {
                        groupedByScenario.computeIfAbsent(scenarioName, k -> new ArrayList<>()).add(resource);
                    }
                }
            }

            // 2. Map grouped files into Scenario objects
            return groupedByScenario.entrySet().stream()
                    .map(entry -> {
                        String scenarioName = entry.getKey();
                        List<Resource> scenarioFiles = entry.getValue();

                        List<ScenarioFile> mappedFiles = scenarioFiles.stream()
                                .map(res -> {
                                    String fileName = res.getFilename();
                                    HarnessType fileType = null;
                                    JsonNode jsonNode;

                                    try (InputStream is = res.getInputStream()) {
                                        // Parse the file into a JSON tree structure
                                        jsonNode = objectMapper.readTree(is);

                                        // Extract the "type" property from the JSON root
                                        if (jsonNode != null && jsonNode.has("type")) {
                                            String typeStr = jsonNode.get("type").asString();
                                            try {
                                                fileType = HarnessType.valueOf(typeStr.toUpperCase());
                                            } catch (IllegalArgumentException e) {
                                                throw new RuntimeException("Unknown HarnessType defined in JSON: " + typeStr, e);
                                            }
                                        }
                                    } catch (Exception e) {
                                        throw new RuntimeException("Failed to parse JSON file: " + fileName, e);
                                    }

                                    return new ScenarioFile(res, fileName, fileType, jsonNode);
                                })
                                // Sort files alphabetically by filename
                                .sorted(Comparator.comparing(ScenarioFile::fileName, Comparator.nullsLast(String::compareTo)))
                                .collect(Collectors.toList());

                        Resource dirResource = directoryMapping.getOrDefault(
                                scenarioName,
                                resolver.getResource("classpath:" + SCENARIOS_DIRECTORY + "/" + scenarioName)
                        );

                        return new Scenario(scenarioName, dirResource, mappedFiles);
                    })
                    // Sort scenarios alphabetically by folder name
                    .sorted(Comparator.comparing(Scenario::name))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            throw new IllegalStateException("Failed to discover scenarios", e);
        }
    }
}