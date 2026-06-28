package com.neu.CoursePlatform.agentic;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Agentic 服务 HTTP 客户端。
 * 所有模块通过此客户端调用 agentic 的 LLM 推理能力。
 *
 * 同时提供：
 * - 按规范定义的专用方法（clusterProblems, teachingSuggestions 等）
 * - 通用 invoke(capability, request) 方法供灵活调用
 */
@Component
public class AgenticClient {

    private final RestTemplate restTemplate;

    @Value("${agentic.base-url:http://localhost:8081}")
    private String baseUrl;

    @Value("${agentic.mode:mock}")
    private String mode;

    public AgenticClient() {
        this.restTemplate = new RestTemplate();
    }

    /** 统一 Agentic 调用入口；mock 模式不依赖外部 AI 服务。 */
    public AgenticResponse invoke(String capability, AgenticRequest request) {
        if ("mock".equalsIgnoreCase(mode)) {
            return new AgenticResponse(true, Map.of("capability", capability, "mock", true), "Mock response");
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

    /**
     * 共性问题聚类（供模块5 T8 使用）
     */
    public String clusterProblems(Map<String, Object> request) throws AgenticException {
        return post("/api/agent/cluster-problems", request);
    }

    /**
     * 教学建议生成（供模块5 T9 使用）
     */
    public String teachingSuggestions(Map<String, Object> request) throws AgenticException {
        return post("/api/agent/teaching-suggestions", request);
    }

    /**
     * 知识图谱提取（供模块1 使用）
     */
    public String knowledgeExtract(Map<String, Object> request) throws AgenticException {
        return post("/api/agent/knowledge-extract", request);
    }

    /**
     * AI 对话（供模块1 PPT讲解/问答 使用）
     */
    public String chat(Map<String, Object> request) throws AgenticException {
        return post("/api/agent/chat", request);
    }

    /**
     * 智能批改（供模块3 使用）
     */
    public String grade(Map<String, Object> request) throws AgenticException {
        return post("/api/agent/grade", request);
    }

    /**
     * 个性化推荐（供模块4 使用）
     */
    public String recommend(Map<String, Object> request) throws AgenticException {
        return post("/api/agent/recommend", request);
    }

    /**
     * 学习风险检测（供模块5 复杂场景兜底使用）
     */
    public String riskDetect(Map<String, Object> request) throws AgenticException {
        return post("/api/agent/risk-detect", request);
    }

    // ---- internal ----

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
