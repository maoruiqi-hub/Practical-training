package com.neu.CoursePlatform.dify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dify 平台 Service API 客户端。
 *
 * 支持的能力：
 * - 对话（Chat）：用于 AI 讲师讲解和答疑，可接入知识库实现 RAG
 * - 文本补全（Completion）：用于结构化任务（评估、推荐等）
 * - 工作流（Workflow）：用于复杂的多步骤 AI 任务
 * - 知识库检索（Knowledge Retrieval）：直接查询知识库
 */
@Component
public class DifyClient {

    private static final Logger log = LoggerFactory.getLogger(DifyClient.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${dify.base-url:http://localhost}")
    private String baseUrl;

    @Value("${dify.chat-api-key:}")
    private String chatApiKey;

    @Value("${dify.completion-api-key:}")
    private String completionApiKey;

    @Value("${dify.workflow-api-key:}")
    private String workflowApiKey;

    @Value("${dify.dataset-id:}")
    private String datasetId;

    @Value("${dify.dataset-api-key:}")
    private String datasetApiKey;

    public DifyClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000);
        requestFactory.setReadTimeout(10000);
        this.restTemplate = new RestTemplate(requestFactory);
        this.objectMapper = new ObjectMapper();
    }

    public boolean isConfigured() {
        return chatApiKey != null && !chatApiKey.isBlank();
    }

    // ---- Chat API ----

    /**
     * 发送对话消息（阻塞模式）。
     * 用于 AI 讲师讲解（lecture）和答疑（qa）。
     */
    public DifyResponse sendChatMessage(String query, String userId) {
        return sendChatMessage(query, userId, null, null);
    }

    /**
     * 发送对话消息，可携带对话上下文变量。
     */
    public DifyResponse sendChatMessage(String query, String userId, String conversationId, Map<String, Object> inputs) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", query);
        body.put("user", userId != null ? userId : "anonymous");
        body.put("response_mode", "blocking");
        body.put("inputs", inputs != null ? new LinkedHashMap<>(inputs) : new LinkedHashMap<>());
        if (conversationId != null && !conversationId.isBlank()) {
            body.put("conversation_id", conversationId);
        }
        return callDify("/v1/chat-messages", chatApiKey, body);
    }

    // ---- Completion API ----

    /**
     * 发送文本补全请求（阻塞模式）。
     * 用于评估、推荐、知识提取等结构化输出任务。
     */
    public DifyResponse sendCompletion(String query, String userId) {
        return sendCompletion(query, userId, null);
    }

    public DifyResponse sendCompletion(String query, String userId, Map<String, Object> inputs) {
        Map<String, Object> body = new LinkedHashMap<>();
        Map<String, Object> allInputs = new LinkedHashMap<>();
        allInputs.put("query", query);
        if (inputs != null && !inputs.isEmpty()) {
            allInputs.putAll(inputs);
        }
        body.put("inputs", allInputs);
        body.put("user", userId != null ? userId : "anonymous");
        body.put("response_mode", "blocking");
        return callDify("/v1/completion-messages", completionApiKey, body);
    }

    // ---- Workflow API ----

    /**
     * 运行工作流（阻塞模式）。
     * 用于复杂的多步骤 AI 任务。
     */
    public DifyResponse runWorkflow(Map<String, Object> inputs, String userId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("inputs", inputs != null ? inputs : Map.of());
        body.put("user", userId != null ? userId : "anonymous");
        body.put("response_mode", "blocking");
        return callDify("/v1/workflows/run", workflowApiKey, body);
    }

    // ---- Knowledge Retrieval API ----

    /**
     * 从知识库中检索相关内容（RAG）。
     */
    public DifyResponse retrieveKnowledge(String query, int topK) {
        if (datasetId == null || datasetId.isBlank()) {
            return DifyResponse.error("Dataset ID not configured");
        }
        Map<String, Object> retrievalModel = new LinkedHashMap<>();
        retrievalModel.put("search_method", "semantic_search");
        Map<String, Object> searchParams = new LinkedHashMap<>();
        searchParams.put("top_k", topK);
        retrievalModel.put("semantic_search", searchParams);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", query);
        body.put("retrieval_model", retrievalModel);

        String apiKey = (datasetApiKey != null && !datasetApiKey.isBlank()) ? datasetApiKey : chatApiKey;
        return callDify("/v1/datasets/" + datasetId + "/retrieve", apiKey, body);
    }

    // ---- Internal ----

    private DifyResponse callDify(String path, String apiKey, Map<String, Object> body) {
        if (apiKey == null || apiKey.isBlank()) {
            return DifyResponse.error("Dify API key not configured for: " + path);
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> resp = restTemplate.exchange(
                    baseUrl + path, HttpMethod.POST, entity, String.class);

            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                return parseResponse(resp.getBody());
            }
            return DifyResponse.error("Dify API returned status: " + resp.getStatusCode());
        } catch (Exception e) {
            log.warn("Dify API call failed for {}: {}", path, e.getMessage());
            return DifyResponse.error("Dify service unavailable: " + e.getMessage());
        }
    }

    private DifyResponse parseResponse(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);

            // Chat response format: { answer, conversation_id, message_id }
            String answer = root.path("answer").asText(null);
            if (answer != null) {
                DifyResponse resp = DifyResponse.success(answer);
                resp.setConversationId(root.path("conversation_id").asText(null));
                resp.setMessageId(root.path("message_id").asText(null));
                return resp;
            }

            // Completion/Workflow response format: { text / data }
            String text = root.path("text").asText(null);
            if (text != null) {
                return DifyResponse.success(text);
            }

            // Workflow response: { data: { text: "..." } }
            JsonNode data = root.path("data");
            if (!data.isMissingNode()) {
                String dataText = data.path("text").asText(null);
                if (dataText != null) {
                    return DifyResponse.success(dataText);
                }
                // For complex workflow outputs, return the entire data as text
                return DifyResponse.success(data.toString());
            }

            // Knowledge retrieval response
            JsonNode records = root.path("records");
            if (records.isArray()) {
                return DifyResponse.success(records.toString());
            }

            return DifyResponse.success(body);
        } catch (Exception e) {
            return DifyResponse.success(body);
        }
    }
}
