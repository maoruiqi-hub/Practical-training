package com.neu.CoursePlatform.agentic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AgenticClient {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient restClient = RestClient.create();

    @Value("${agentic.api-key:}")
    private String apiKey;

    @Value("${agentic.base-url:}")
    private String baseUrl;

    @Value("${agentic.model:gpt-4o-mini}")
    private String model;

    public String generateAssessmentJson(String prompt) throws Exception {
        if (apiKey == null || apiKey.isBlank() || baseUrl == null || baseUrl.isBlank()) {
            return null;
        }
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", model);
        request.put("temperature", 0.2);
        request.put("messages", List.of(
                Map.of("role", "system", "content", "你是课程平台的学习成果评价助手。只返回严格 JSON，不要使用 Markdown。"),
                Map.of("role", "user", "content", prompt)
        ));
        String response = restClient.post()
                .uri(baseUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .body(request)
                .retrieve()
                .body(String.class);
        return extractModelContent(response);
    }

    private String extractModelContent(String response) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        JsonNode choices = root.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            return choices.get(0).path("message").path("content").asText();
        }
        JsonNode outputText = root.path("output_text");
        if (!outputText.isMissingNode()) return outputText.asText();
        return response;
    }
}
