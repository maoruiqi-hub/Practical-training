package com.neu.CoursePlatform.agentic;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * The HTTP contract is deliberately small: the future Agentic service receives
 * the course/resource identifiers and returns an {@link AgenticResponse}.
 * Mock mode is only for local development and returns data in the same shape.
 */
@Component
public class AgenticClientImpl implements AgenticClient {

    private final String mode;
    private final String baseUrl;
    private final RestTemplate restTemplate = new RestTemplate();

    public AgenticClientImpl(@Value("${agentic.mode:mock}") String mode,
                             @Value("${agentic.base-url:}") String baseUrl) {
        this.mode = mode;
        this.baseUrl = baseUrl;
    }

    @Override
    public AgenticResponse invoke(String capability, AgenticRequest request) {
        if ("mock".equalsIgnoreCase(mode)) return mockResponse(capability, request);
        if (!"http".equalsIgnoreCase(mode) || baseUrl.isBlank()) return AgenticResponse.unavailable();
        try {
            ResponseEntity<AgenticResponse> response = restTemplate.postForEntity(
                    baseUrl + "/" + capability, request, AgenticResponse.class);
            return response.getBody() == null ? AgenticResponse.unavailable() : response.getBody();
        } catch (Exception ignored) {
            return AgenticResponse.unavailable();
        }
    }

    private AgenticResponse mockResponse(String capability, AgenticRequest request) {
        if ("extract".equals(capability)) {
            String chapter = request.getContext() == null ? null : String.valueOf(request.getContext().get("chapter"));
            return new AgenticResponse(true, Map.of("knowledgePoints", List.of(
                    Map.of("name", "课程资料核心概念", "description", "本地联调用的示例候选知识点，接入真实 Agentic 服务后将替换为资料解析结果。", "chapter", chapter == null ? "" : chapter, "importance", 3),
                    Map.of("name", "课程资料关键术语", "description", "请由教师审核并修改后再纳入课程知识图谱。", "chapter", chapter == null ? "" : chapter, "importance", 2)
            ), "mock", true), "Mock extraction response");
        }
        if ("lecture".equals(capability)) {
            return new AgenticResponse(true, Map.of(
                    "explanation", "这是针对当前知识点和 PPT 页面的本地联调讲解。",
                    "learningFocus", "理解概念定义与适用场景。",
                    "commonMistakes", "不要只记结论，应结合例子判断。",
                    "example", "请尝试用自己的话复述该概念。",
                    "mock", true), "Mock lecture response");
        }
        if ("qa".equals(capability)) {
            return new AgenticResponse(true, Map.of(
                    "answer", "这是本地联调用回答。接入真实 Agentic 检索服务后，会基于当前课程资料和知识点生成带来源引用的答案。",
                    "citations", List.of(),
                    "mock", true), "Mock QA response");
        }
        return new AgenticResponse(true, Map.of("capability", capability, "mock", true), "Mock agentic response");
    }
}
