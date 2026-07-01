package com.neu.CoursePlatform.service.impl;

import com.neu.CoursePlatform.agentic.AgenticClient;
import com.neu.CoursePlatform.agentic.AgenticRequest;
import com.neu.CoursePlatform.agentic.AgenticResponse;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.dto.CourseQaRequest;
import com.neu.CoursePlatform.dto.LectureRequest;
import com.neu.CoursePlatform.entity.CourseResource;
import com.neu.CoursePlatform.entity.KnowledgePoint;
import com.neu.CoursePlatform.service.CourseAiService;
import com.neu.CoursePlatform.service.CourseResourceService;
import com.neu.CoursePlatform.service.KnowledgePointService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CourseAiServiceImpl implements CourseAiService {
    private final AgenticClient agenticClient;
    private final KnowledgePointService knowledgePointService;
    private final CourseResourceService courseResourceService;

    public CourseAiServiceImpl(AgenticClient agenticClient,
                               KnowledgePointService knowledgePointService,
                               CourseResourceService courseResourceService) {
        this.agenticClient = agenticClient;
        this.knowledgePointService = knowledgePointService;
        this.courseResourceService = courseResourceService;
    }

    @Override
    public Result<AgenticResponse> explainKnowledgePoint(String knowledgePointId, LectureRequest request) {
        KnowledgePoint point = knowledgePointService.getById(knowledgePointId);
        if (point == null) return Result.fail("知识点不存在");
        if (request == null || request.getResourceId() == null) return Result.fail("请选择 PPT 资源");

        CourseResource resource = courseResourceService.getById(request.getResourceId());
        if (resource == null
                || !point.getCourseCode().equals(resource.getCourseCode())
                || !"ppt".equals(resource.getResourceType())) {
            return Result.fail("PPT 资源不属于该知识点课程");
        }

        AgenticRequest input = new AgenticRequest();
        input.setCourseCode(point.getCourseCode());
        input.setResourceId(resource.getResourceId());
        input.setKnowledgePointId(knowledgePointId);
        input.setContent(request.getQuestion());
        input.setContext(Map.of(
                "pageNumber", request.getPageNumber() == null ? 0 : request.getPageNumber(),
                "previousMessages", request.getPreviousMessages() == null ? List.of() : request.getPreviousMessages(),
                "knowledgePoint", point.getName()
        ));

        AgenticResponse response = normalizeChatResponse(agenticClient.invoke("lecture", input));
        return response.isSuccess() ? Result.ok(response) : Result.serviceUnavailable("AI 服务暂不可用");
    }

    @Override
    public Result<AgenticResponse> answerKnowledgePointQuestion(String knowledgePointId, CourseQaRequest request) {
        KnowledgePoint point = knowledgePointService.getById(knowledgePointId);
        if (point == null) return Result.fail("知识点不存在");
        if (request == null || request.getQuestion() == null || request.getQuestion().isBlank()) {
            return Result.fail("问题不能为空");
        }

        CourseResource resource = null;
        if (request.getResourceId() != null && !request.getResourceId().isBlank()) {
            resource = courseResourceService.getById(request.getResourceId());
            if (resource == null || !point.getCourseCode().equals(resource.getCourseCode())) {
                return Result.fail("课程资料不属于当前课程");
            }
        }

        AgenticRequest input = new AgenticRequest();
        input.setCourseCode(point.getCourseCode());
        input.setKnowledgePointId(knowledgePointId);
        input.setResourceId(resource == null ? null : resource.getResourceId());
        input.setContent(request.getQuestion());
        input.setContext(Map.of(
                "knowledgePoint", point.getName(),
                "resourceTitle", resource == null ? "课程知识图谱" : resource.getTitle(),
                "previousMessages", request.getPreviousMessages() == null ? List.of() : request.getPreviousMessages()
        ));
        AgenticResponse response = normalizeChatResponse(agenticClient.invoke("qa", input));
        return response.isSuccess() ? Result.ok(response) : Result.serviceUnavailable("AI 服务暂不可用");
    }

    @Override
    public Result<AgenticResponse> generateAbilityMap(String courseCode) {
        List<KnowledgePoint> knowledgePoints = knowledgePointService.listByCourseCode(courseCode, null);
        AgenticRequest request = new AgenticRequest();
        request.setCourseCode(courseCode);
        request.setContext(Map.of("knowledgePoints", knowledgePoints));
        AgenticResponse response = agenticClient.invoke("ability-map", request);
        return response.isSuccess() ? Result.ok(response) : Result.serviceUnavailable("AI 服务暂不可用");
    }

    private AgenticResponse normalizeChatResponse(AgenticResponse response) {
        if (response == null || !response.isSuccess()) return response;
        Map<String, Object> rawData = response.getData() == null ? Map.of() : response.getData();
        Map<String, Object> data = new LinkedHashMap<>(rawData);
        if (!hasText(data.get("answer"))) {
            String answer = firstText(data.get("content"), data.get("text"), data.get("result"));
            if (answer == null && !isStatusMessage(response.getMessage())) {
                answer = response.getMessage();
            }
            if (answer != null) data.put("answer", answer);
        }
        return new AgenticResponse(response.isSuccess(), data, response.getMessage());
    }

    private String firstText(Object... values) {
        for (Object value : values) {
            if (hasText(value)) return String.valueOf(value).trim();
        }
        return null;
    }

    private boolean hasText(Object value) {
        return value != null && !String.valueOf(value).isBlank();
    }

    private boolean isStatusMessage(String message) {
        if (message == null) return true;
        String normalized = message.trim().toLowerCase();
        return normalized.isBlank() || "ok".equals(normalized) || "success".equals(normalized);
    }
}
