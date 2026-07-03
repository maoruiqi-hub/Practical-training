package com.neu.CoursePlatform.agentic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neu.CoursePlatform.dify.DifyClient;
import com.neu.CoursePlatform.dify.DifyKnowledgeService;
import com.neu.CoursePlatform.dify.DifyResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agentic 服务 HTTP 客户端。
 * 所有模块通过此客户端调用 AI 推理能力。
 *
 * 支持四种模式：
 * - mock:     返回模拟数据，不依赖外部服务
 * - deepseek: 调用 DeepSeek Anthropic-compatible API
 * - dify:     通过 Dify 平台调用 LLM，支持 RAG/Workflow/Agent
 * - http:     调用外部 agentic 微服务
 */
@Component
public class AgenticClient {

    private static final Logger log = LoggerFactory.getLogger(AgenticClient.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final DifyClient difyClient;
    private final DifyKnowledgeService difyKnowledgeService;

    @Value("${agentic.base-url:http://localhost:8081}")
    private String baseUrl;

    @Value("${agentic.mode:mock}")
    private String mode;

    @Value("${agentic.api-key:}")
    private String apiKey;

    @Value("${agentic.model:deepseek-v4-pro}")
    private String model;

    public AgenticClient(DifyClient difyClient, DifyKnowledgeService difyKnowledgeService) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(45000);
        this.restTemplate = new RestTemplate(requestFactory);
        this.objectMapper = new ObjectMapper();
        this.difyClient = difyClient;
        this.difyKnowledgeService = difyKnowledgeService;
    }

    /** 统一 Agentic 调用入口 */
    public AgenticResponse invoke(String capability, AgenticRequest request) {
        if ("mock".equalsIgnoreCase(mode)) {
            return new AgenticResponse(true, Map.of("capability", capability, "mock", true), "Mock response");
        }
        if ("dify".equalsIgnoreCase(mode)) {
            return invokeDify(capability, request);
        }
        if ("deepseek".equalsIgnoreCase(mode)) {
            return invokeDeepSeek(capability, request);
        }
        if (!"http".equalsIgnoreCase(mode) || baseUrl == null || baseUrl.isBlank()) {
            return AgenticResponse.unavailable();
        }
        try {
            Map<String, Object> body = Map.of("capability", capability, "request", request);
            String result = post("/api/agent/" + capability, body);
            return new AgenticResponse(true, Map.of("result", result), "ok");
        } catch (AgenticException e) {
            return AgenticResponse.unavailable();
        }
    }

    public boolean isMockMode() {
        return "mock".equalsIgnoreCase(mode);
    }

    public boolean isConfiguredForRealAi() {
        if ("deepseek".equalsIgnoreCase(mode)) {
            return apiKey != null && !apiKey.isBlank()
                    && baseUrl != null && !baseUrl.isBlank();
        }
        if ("dify".equalsIgnoreCase(mode)) {
            return difyClient.isConfigured();
        }
        if ("http".equalsIgnoreCase(mode)) {
            return baseUrl != null && !baseUrl.isBlank();
        }
        return false;
    }

    public String configurationMessage() {
        if (isMockMode()) return "当前为 mock 模式，仅用于开发联调";
        if ("deepseek".equalsIgnoreCase(mode)) return "DeepSeek/Anthropic API token 或 base-url 未配置";
        if ("dify".equalsIgnoreCase(mode)) return "Dify API key 或服务地址未配置";
        if ("http".equalsIgnoreCase(mode)) return "外部 agentic 服务地址未配置";
        return "agentic.mode 未配置为 deepseek、dify 或 http";
    }

    /**
     * 共性问题聚类（供模块5 T8 使用）
     */
    public String clusterProblems(Map<String, Object> request) throws AgenticException {
        if ("mock".equalsIgnoreCase(mode)) {
            return "[{\"topic\":\"Mock聚类示例\",\"student_count\":5,\"description\":\"Mock模式下的示例聚类结果\"}]";
        }
        if ("dify".equalsIgnoreCase(mode)) {
            try {
                DifyResponse resp = difyClient.sendChatMessage(objectMapper.writeValueAsString(request), "system");
                if (resp.isSuccess()) return resp.getContent();
                throw new AgenticException("Dify clusterProblems failed: " + resp.getError());
            } catch (Exception e) {
                throw new AgenticException("Dify clusterProblems failed: " + e.getMessage(), e);
            }
        }
        if ("deepseek".equalsIgnoreCase(mode)) {
            try {
                return callDeepSeek(getSystemPrompt("clusterProblems"),
                        objectMapper.writeValueAsString(request));
            } catch (Exception e) {
                throw new AgenticException("DeepSeek clusterProblems failed: " + e.getMessage(), e);
            }
        }
        return post("/api/agent/cluster-problems", request);
    }

    /**
     * 教学建议生成（供模块5 T9 使用）
     */
    public String teachingSuggestions(Map<String, Object> request) throws AgenticException {
        if ("mock".equalsIgnoreCase(mode)) {
            return "[{\"suggestion_type\":\"reteach\",\"content\":\"Mock模式建议：根据薄弱知识点进行针对性复习\",\"target\":\"whole_class\",\"urgency\":\"medium\"}]";
        }
        if ("dify".equalsIgnoreCase(mode)) {
            try {
                DifyResponse resp = difyClient.sendChatMessage(objectMapper.writeValueAsString(request), "system");
                if (resp.isSuccess()) return resp.getContent();
                throw new AgenticException("Dify teachingSuggestions failed: " + resp.getError());
            } catch (Exception e) {
                throw new AgenticException("Dify teachingSuggestions failed: " + e.getMessage(), e);
            }
        }
        if ("deepseek".equalsIgnoreCase(mode)) {
            try {
                return callDeepSeek(getSystemPrompt("teachingSuggestions"),
                        objectMapper.writeValueAsString(request));
            } catch (Exception e) {
                throw new AgenticException("DeepSeek teachingSuggestions failed: " + e.getMessage(), e);
            }
        }
        return post("/api/agent/teaching-suggestions", request);
    }

    /**
     * 知识图谱提取（供模块1 使用）
     */
    public String knowledgeExtract(Map<String, Object> request) throws AgenticException {
        if ("dify".equalsIgnoreCase(mode)) {
            try {
                DifyResponse resp = difyClient.sendChatMessage(objectMapper.writeValueAsString(request), "system");
                if (resp.isSuccess()) return resp.getContent();
                throw new AgenticException("Dify knowledgeExtract failed: " + resp.getError());
            } catch (Exception e) {
                throw new AgenticException("Dify knowledgeExtract failed: " + e.getMessage(), e);
            }
        }
        if ("deepseek".equalsIgnoreCase(mode)) {
            try {
                return callDeepSeek(getSystemPrompt("extract"),
                        objectMapper.writeValueAsString(request));
            } catch (Exception e) {
                throw new AgenticException("DeepSeek knowledgeExtract failed: " + e.getMessage(), e);
            }
        }
        return post("/api/agent/knowledge-extract", request);
    }

    /**
     * AI 对话（供模块1 PPT讲解/问答 使用）
     */
    public String chat(Map<String, Object> request) throws AgenticException {
        if ("dify".equalsIgnoreCase(mode)) {
            try {
                DifyResponse resp = difyClient.sendChatMessage(objectMapper.writeValueAsString(request), "anonymous");
                if (resp.isSuccess()) return resp.getContent();
                throw new AgenticException("Dify chat failed: " + resp.getError());
            } catch (Exception e) {
                throw new AgenticException("Dify chat failed: " + e.getMessage(), e);
            }
        }
        if ("deepseek".equalsIgnoreCase(mode)) {
            try {
                return callDeepSeek(getSystemPrompt("lecture"),
                        objectMapper.writeValueAsString(request));
            } catch (Exception e) {
                throw new AgenticException("DeepSeek chat failed: " + e.getMessage(), e);
            }
        }
        return post("/api/agent/chat", request);
    }

    /**
     * 智能批改（供模块3 使用）
     */
    public String grade(Map<String, Object> request) throws AgenticException {
        if ("dify".equalsIgnoreCase(mode)) {
            try {
                DifyResponse resp = difyClient.sendChatMessage(objectMapper.writeValueAsString(request), "system");
                if (resp.isSuccess()) return resp.getContent();
                throw new AgenticException("Dify grade failed: " + resp.getError());
            } catch (Exception e) {
                throw new AgenticException("Dify grade failed: " + e.getMessage(), e);
            }
        }
        if ("deepseek".equalsIgnoreCase(mode)) {
            try {
                return callDeepSeek(getSystemPrompt("assessment"),
                        objectMapper.writeValueAsString(request));
            } catch (Exception e) {
                throw new AgenticException("DeepSeek grade failed: " + e.getMessage(), e);
            }
        }
        return post("/api/agent/grade", request);
    }

    /**
     * 个性化推荐（供模块4 使用）
     */
    public String recommend(Map<String, Object> request) throws AgenticException {
        if ("dify".equalsIgnoreCase(mode)) {
            try {
                DifyResponse resp = difyClient.sendChatMessage(objectMapper.writeValueAsString(request), "system");
                if (resp.isSuccess()) return resp.getContent();
                throw new AgenticException("Dify recommend failed: " + resp.getError());
            } catch (Exception e) {
                throw new AgenticException("Dify recommend failed: " + e.getMessage(), e);
            }
        }
        if ("deepseek".equalsIgnoreCase(mode)) {
            try {
                return callDeepSeek(getSystemPrompt("recommend"),
                        objectMapper.writeValueAsString(request));
            } catch (Exception e) {
                throw new AgenticException("DeepSeek recommend failed: " + e.getMessage(), e);
            }
        }
        return post("/api/agent/recommend", request);
    }

    /**
     * 学习风险检测（供模块5 复杂场景兜底使用）
     */
    public String riskDetect(Map<String, Object> request) throws AgenticException {
        if ("dify".equalsIgnoreCase(mode)) {
            try {
                DifyResponse resp = difyClient.sendChatMessage(objectMapper.writeValueAsString(request), "system");
                if (resp.isSuccess()) return resp.getContent();
                throw new AgenticException("Dify riskDetect failed: " + resp.getError());
            } catch (Exception e) {
                throw new AgenticException("Dify riskDetect failed: " + e.getMessage(), e);
            }
        }
        if ("deepseek".equalsIgnoreCase(mode)) {
            try {
                return callDeepSeek(getSystemPrompt("riskDetect"),
                        objectMapper.writeValueAsString(request));
            } catch (Exception e) {
                throw new AgenticException("DeepSeek riskDetect failed: " + e.getMessage(), e);
            }
        }
        return post("/api/agent/risk-detect", request);
    }

    // ---- dify mode internals ----

    private AgenticResponse invokeDify(String capability, AgenticRequest request) {
        if (!difyClient.isConfigured()) {
            log.warn("Dify not configured, falling back to mock for capability: {}", capability);
            return new AgenticResponse(true, Map.of("capability", capability, "mock", true, "fallback", true),
                    "Dify not configured, using mock response");
        }
        try {
            String userMessage = buildUserMessage(capability, request);
            String userId = request.getContext() != null ?
                    String.valueOf(request.getContext().getOrDefault("userId", "anonymous")) : "anonymous";
            String courseCode = request.getCourseCode();
            String knowledgePointId = request.getKnowledgePointId();

            // 统一走 Chat API — lecture/qa 通过 RAG 增强，结构化任务直接发送
            if ("lecture".equals(capability) || "qa".equals(capability)) {
                String ragQuery = difyKnowledgeService.buildRagPrompt(userMessage, courseCode, knowledgePointId);
                DifyResponse resp = difyClient.sendChatMessage(ragQuery, userId);
                return difyToAgentic(resp);
            }
            DifyResponse resp = difyClient.sendChatMessage(userMessage, userId);
            return difyToAgentic(resp);
        } catch (Exception e) {
            log.warn("Dify invoke failed for capability {}: {}", capability, e.getMessage());
            return AgenticResponse.unavailable();
        }
    }

    private AgenticResponse difyToAgentic(DifyResponse difyResp) {
        if (!difyResp.isSuccess()) {
            return AgenticResponse.unavailable();
        }
        return new AgenticResponse(true,
                Map.of("result", difyResp.getContent(), "conversationId",
                        difyResp.getConversationId() != null ? difyResp.getConversationId() : ""),
                "ok");
    }

    // ---- deepseek mode internals ----

    private AgenticResponse invokeDeepSeek(String capability, AgenticRequest request) {
        try {
            String systemPrompt = getSystemPrompt(capability);
            String userMessage = buildUserMessage(capability, request);
            String rawResponse = callDeepSeek(systemPrompt, userMessage);
            Map<String, Object> data = parseDeepSeekResponse(capability, rawResponse);
            return new AgenticResponse(true, data, "ok");
        } catch (Exception e) {
            log.warn("DeepSeek API call failed for capability {}: {}", capability, e.getMessage());
            return AgenticResponse.unavailable();
        }
    }

    private String callDeepSeek(String systemPrompt, String userMessage) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("max_tokens", 4096);
        body.put("system", systemPrompt);
        body.put("messages", List.of(Map.of("role", "user", "content", userMessage)));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> resp = restTemplate.exchange(
                baseUrl + "/v1/messages", HttpMethod.POST, entity, String.class);

        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new AgenticException("DeepSeek API returned status: " + resp.getStatusCode());
        }

        JsonNode root = objectMapper.readTree(resp.getBody());

        // 检查是否为错误响应
        if (root.has("type") && "error".equals(root.path("type").asText())) {
            String errMsg = root.path("error").path("message").asText("unknown error");
            throw new AgenticException("DeepSeek API error: " + errMsg);
        }

        JsonNode content = root.path("content");
        if (content.isArray()) {
            for (JsonNode block : content) {
                if ("text".equals(block.path("type").asText())) {
                    String text = block.path("text").asText();
                    if (text != null && !text.isBlank()) {
                        return text;
                    }
                }
            }
        }
        throw new AgenticException("DeepSeek response has no text content");
    }

    private String buildUserMessage(String capability, AgenticRequest request) {
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("capability", capability);
        if (request.getContent() != null && !request.getContent().isBlank()) {
            msg.put("input", request.getContent());
        }
        if (request.getContext() != null && !request.getContext().isEmpty()) {
            msg.putAll(request.getContext());
        }
        if (request.getCourseCode() != null && !request.getCourseCode().isBlank()) {
            msg.put("courseCode", request.getCourseCode());
        }
        if (request.getKnowledgePointId() != null && !request.getKnowledgePointId().isBlank()) {
            msg.put("knowledgePointId", request.getKnowledgePointId());
        }
        try {
            return objectMapper.writeValueAsString(msg);
        } catch (Exception e) {
            return request.getContent() != null ? request.getContent() : "";
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseDeepSeekResponse(String capability, String raw) {
        // 尝试解析为 JSON；解析失败则将原始文本作为 result 返回
        String trimmed = extractJson(raw);
        if (trimmed.startsWith("{")) {
            try {
                JsonNode node = objectMapper.readTree(trimmed);
                Map<String, Object> result = objectMapper.convertValue(node, Map.class);
                if (!result.isEmpty()) return result;
            } catch (Exception ignored) {
            }
        }
        if (trimmed.startsWith("[")) {
            try {
                JsonNode node = objectMapper.readTree(trimmed);
                return Map.of("items", objectMapper.convertValue(node, List.class));
            } catch (Exception ignored) {
            }
        }
        return Map.of("result", raw);
    }

    private String extractJson(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```(?:json)?\\s*", "")
                    .replaceFirst("\\s*```$", "")
                    .trim();
        }
        int arrayStart = text.indexOf('[');
        int objectStart = text.indexOf('{');
        int start;
        if (arrayStart >= 0 && objectStart >= 0) start = Math.min(arrayStart, objectStart);
        else start = Math.max(arrayStart, objectStart);
        if (start < 0) return text;
        int end = text.charAt(start) == '[' ? text.lastIndexOf(']') : text.lastIndexOf('}');
        if (end < start) return text.substring(start);
        return text.substring(start, end + 1).trim();
    }

    private String getSystemPrompt(String capability) {
        return switch (capability) {
            case "lecture" -> "你是一个大学课程AI讲师，根据PPT内容和知识点为学生讲解概念。用清晰易懂的语言，结合例子说明。回答使用中文。";
            case "qa" -> "你是一个课程答疑助手，回答学生关于知识点的问题。回答应准确、简洁，引用课程资料中的内容。回答使用中文。";
            case "ability-map" -> """
                    根据课程知识点列表生成能力图谱JSON。请把知识点归纳为4-8个能力点，每个能力点绑定相关知识点。
                    严格返回如下格式，不要返回 Markdown：
                    {"abilityPoints": [{"name": "能力点名称", "description": "能力说明", "knowledgePointIds": ["输入中的knowledgePointId"]}]}
                    knowledgePointIds 只能使用输入知识点里的 knowledgePointId。
                    """;
            case "extract" -> """
                    从课程资源中提取知识点。严格返回如下JSON：
                    {"knowledgePoints": [{"name": "知识点名称", "description": "简要说明", "chapter": "章节名", "importance": 1-5的整数}]}
                    """;
            case "assessment" -> """
                    你是一个作业评阅助手。根据任务要求和学生提交内容，给出评分和修改建议。严格返回如下JSON：
                    {"score": 0-100整数, "dimensions": {"内容完整性": 0-100, "知识点覆盖度": 0-100, "逻辑结构": 0-100, "表达规范": 0-100, "任务要求符合度": 0-100}, "summary": "评语摘要", "suggestions": ["建议1", "建议2"], "riskLevel": "low|medium|high"}
                    """;
            case "recommend" -> "根据学生学习数据生成个性化推荐理由。直接返回推荐理由文本，30字以内。";
            case "clusterProblems" -> """
                    分析学生错题数据，识别共性问题和薄弱环节。返回JSON数组：
                    [{"topic": "共性问题主题", "student_count": 受影响学生数, "knowledge_points": ["相关知识点"], "description": "问题描述", "suggested_action": "建议措施"}]
                    """;
            case "teachingSuggestions" -> """
                    根据班级学情数据（薄弱点、进度、风险学生），生成教学干预建议。返回JSON数组：
                    [{"suggestion_type": "reteach|practice|individual|pace", "content": "具体建议内容", "target": "whole_class|group|individual", "urgency": "high|medium|low", "based_on": "依据的数据特征"}]
                    """;
            case "riskDetect" -> """
                    分析学生学习数据，检测学习风险。返回JSON：
                    {"risks": [{"type": "procrastination|low_score|inactive|progress_lag", "level": "high|medium|low", "detail": "详细描述"}]}
                    """;
            case "tower-diagnosis-report" -> """
                    你是课程爬塔系统的 AI 诊断导师。根据学生在侦察房或战斗房的答题记录，返回严格 JSON：
                    {"summary":"一句话诊断","weaknesses":["薄弱点1"],"recommendedAction":"下一步建议","reviewFocus":["复习重点1"],"source":"real_ai"}
                    只输出 JSON，不要输出 Markdown。回答使用中文，建议必须针对错题和知识点。
                    """;
            default -> "你是一个教育AI助手，帮助教师和学生完成教学任务。回答使用中文，简洁准确。";
        };
    }

    // ---- internal http helpers ----

    private String post(String path, Map<String, Object> body) throws AgenticException {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> resp = restTemplate.exchange(
                    baseUrl + path, HttpMethod.POST, entity, String.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                return resp.getBody();
            }
            throw new AgenticException("agentic returned status: " + resp.getStatusCode());
        } catch (RestClientException e) {
            throw new AgenticException("agentic service unavailable: " + e.getMessage(), e);
        }
    }

    /** Agentic 服务异常 */
    public static class AgenticException extends Exception {
        public AgenticException(String msg) { super(msg); }
        public AgenticException(String msg, Throwable cause) { super(msg, cause); }
    }
}
