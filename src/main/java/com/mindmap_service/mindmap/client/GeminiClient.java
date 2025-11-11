package com.mindmap_service.mindmap.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mindmap_service.mindmap.exception.MindmapAiException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GeminiClient {

    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("(?s)```(?:\\w+)?\\n(.*?)```");

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private final GeminiProperties properties;
    private final ObjectMapper objectMapper;

    private HttpClient httpClient;

    @PostConstruct
    void init() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public JsonNode generateMindmap(UUID userId, String prompt, String project) {
        validateConfiguration();
        String decoratedPrompt = decoratePrompt(userId, prompt, project);
        GenerateContentRequest requestPayload =
                new GenerateContentRequest(List.of(new Content(List.of(new ContentPart(decoratedPrompt)))));

        String rawRequest;
        try {
            rawRequest = objectMapper.writeValueAsString(requestPayload);
        } catch (IOException ex) {
            throw new MindmapAiException("Failed to serialize Gemini request", ex);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(buildRequestUrl()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(rawRequest))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new MindmapAiException("Failed to call Gemini AI", ex);
        }

        if (response.statusCode() != 200) {
            throw new MindmapAiException(
                    "Gemini AI returned unexpected status " + response.statusCode() + ": " + response.body());
        }

        return extractMindmap(response.body(), prompt);
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(properties.getUrl())) {
            throw new MindmapAiException("Gemini endpoint is not configured");
        }
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new MindmapAiException("Gemini API key is not configured");
        }
    }

    private String buildRequestUrl() {
        String base = properties.getUrl();
        String separator = base.contains("?") ? "&" : "?";
        return base + separator + "key=" + properties.getApiKey();
    }

    private String decoratePrompt(UUID userId, String prompt, String project) {
        String template = properties.getPromptTemplate();
        return template.replace("{prompt}", prompt)
                .replace("{project}", project == null ? "general" : project)
                .replace("{userId}", userId.toString());
    }

    private JsonNode extractMindmap(String payload, String prompt) {
        JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (IOException ex) {
            throw new MindmapAiException("Failed to parse Gemini response", ex);
        }

        if (root.has("error")) {
            JsonNode error = root.get("error");
            throw new MindmapAiException("Gemini AI returned error: " + error.toString());
        }

        JsonNode candidates = root.path("candidates");
        System.out.println(candidates.size());
        if (!candidates.isArray() || candidates.isEmpty()) {
            throw new MindmapAiException("Gemini response did not contain any candidates");
        }

        JsonNode candidate = candidates.get(0);
        String rawMindmap = extractTextFromCandidate(candidate);
        if (!StringUtils.hasText(rawMindmap)) {
            throw new MindmapAiException("Gemini response was empty");
        }

        JsonNode mindmap = parseMindmapPayload(rawMindmap);
        JsonNode normalized = ensureMindmapStructure(mindmap, prompt);
        validateStructure(normalized);
        return normalized;
    }

    private JsonNode parseMindmapPayload(String rawMindmap) {
        String cleaned = stripMarkdown(rawMindmap);
        try {
            return objectMapper.readTree(cleaned);
        } catch (IOException ex) {
            JsonNode snippet = extractJsonSnippet(cleaned);
            if (snippet != null) {
                return snippet;
            }
            throw new MindmapAiException("Gemini did not return valid JSON; response was: " + cleaned, ex);
        }
    }

    private JsonNode ensureMindmapStructure(JsonNode candidate, String prompt) {
        if (hasNodes(candidate)) {
            return candidate;
        }
        if (candidate.isArray()) {
            return buildMindmapFromArray(candidate, prompt);
        }
        return candidate;
    }

    private void attachChildHierarchy(ObjectNode root) {
        JsonNode nodesNode = root.get("nodes");
        if (!(nodesNode instanceof ArrayNode nodes)) {
            return;
        }
        Map<String, ObjectNode> lookup = new HashMap<>();
        for (JsonNode node : nodes) {
            if (node instanceof ObjectNode objectNode && objectNode.hasNonNull("id")) {
                objectNode.remove("children");
                lookup.put(objectNode.get("id").asText(), objectNode);
            }
        }

        ArrayNode treeRoots = objectMapper.createArrayNode();
        for (ObjectNode node : lookup.values()) {
            String parentId = node.path("parentId").asText(null);
            if (StringUtils.hasText(parentId) && lookup.containsKey(parentId)) {
                lookup.get(parentId).withArray("children").add(node);
            } else {
                treeRoots.add(node);
            }
        }
        root.set("nodeTree", treeRoots);
    }

    private boolean hasNodes(JsonNode candidate) {
        return candidate.hasNonNull("title")
                && candidate.has("nodes")
                && candidate.get("nodes").isArray();
    }

    private JsonNode buildMindmapFromArray(JsonNode array, String prompt) {
        ObjectNode root = objectMapper.createObjectNode();
        String title = StringUtils.hasText(prompt) ? prompt : "Mindmap";
        root.put("title", title);
        root.put("description", "Generated from Gemini topic list");
        ArrayNode nodes = objectMapper.createArrayNode();
        String rootId = UUID.randomUUID().toString();
        nodes.add(createNode(rootId, title, null, "Derived from topics"));
        if (array.isArray()) {
            for (JsonNode topic : array) {
                String topicId = UUID.randomUUID().toString();
                String topicLabel =
                        topic.path("topic").asText(topic.path("name").asText("Topic"));
                nodes.add(createNode(topicId, topicLabel, rootId, joinDetails(topic.path("details"))));
                JsonNode subtopics = topic.path("subtopics");
                if (subtopics.isArray()) {
                    for (JsonNode subtopic : subtopics) {
                        String subId = UUID.randomUUID().toString();
                        String subLabel = subtopic.path("name").asText("Subtopic");
                        nodes.add(createNode(subId, subLabel, topicId, joinDetails(subtopic.path("details"))));
                    }
                }
            }
        }
        root.set("nodes", nodes);
        return root;
    }

    private String joinDetails(JsonNode details) {
        if (!details.isArray()) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (JsonNode detail : details) {
            if (builder.length() > 0) {
                builder.append("; ");
            }
            builder.append(detail.asText());
        }
        return builder.length() == 0 ? null : builder.toString();
    }

    private ObjectNode createNode(String id, String label, String parentId, String notes) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("id", id);
        node.put("label", label);
        if (parentId != null) {
            node.put("parentId", parentId);
        }
        if (notes != null) {
            node.put("notes", notes);
        }
        return node;
    }

    private String stripMarkdown(String text) {
        if (text == null) {
            return "";
        }
        Matcher matcher = CODE_BLOCK_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return text.trim();
    }

    private JsonNode extractJsonSnippet(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            String snippet = text.substring(start, end + 1).trim();
            if (snippet.isEmpty()) {
                return null;
            }
            try {
                return objectMapper.readTree(snippet);
            } catch (IOException ex) {
                return null;
            }
        }
        return null;
    }

    private String extractTextFromCandidate(JsonNode candidate) {
        JsonNode content = candidate.path("content");
        if (content.isObject()) {
            JsonNode parts = content.path("parts");
            if (parts.isArray()) {
                StringBuilder builder = new StringBuilder();
                for (JsonNode part : parts) {
                    String text = part.path("text").asText(null);
                    if (StringUtils.hasText(text)) {
                        builder.append(text);
                    }
                }
                if (builder.length() > 0) {
                    return builder.toString();
                }
            }
        }

        if (candidate.has("output")) {
            String output = candidate.get("output").asText(null);
            if (StringUtils.hasText(output)) {
                return output;
            }
        }
        return null;
    }

    private void validateStructure(JsonNode structure) {
        if (!structure.hasNonNull("title")
                || !structure.has("nodes")
                || !structure.get("nodes").isArray()) {
            log.warn("Invalid mindmap payload structure: {}", structure);
            throw new MindmapAiException("Mindmap payload must contain title and nodes array");
        }

        for (JsonNode node : structure.get("nodes")) {
            if (!node.hasNonNull("id") || !node.hasNonNull("label")) {
                log.warn("Mindmap node missing id/label: {}", node);
                throw new MindmapAiException("Each node must contain id and label");
            }
        }

        if (structure.isObject()) {
            attachChildHierarchy((ObjectNode) structure);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record GenerateContentRequest(List<Content> contents) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record Content(List<ContentPart> parts) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record ContentPart(String text) {}
}
