package com.neu.CoursePlatform.service.impl;

import com.neu.CoursePlatform.agentic.AgenticClient;
import com.neu.CoursePlatform.agentic.AgenticRequest;
import com.neu.CoursePlatform.agentic.AgenticResponse;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.dto.CourseQaRequest;
import com.neu.CoursePlatform.dto.LectureRequest;
import com.neu.CoursePlatform.entity.AbilityPoint;
import com.neu.CoursePlatform.entity.CourseResource;
import com.neu.CoursePlatform.entity.KnowledgePoint;
import com.neu.CoursePlatform.service.AbilityMapService;
import com.neu.CoursePlatform.service.AbilityPointService;
import com.neu.CoursePlatform.service.CourseAiService;
import com.neu.CoursePlatform.service.CourseResourceService;
import com.neu.CoursePlatform.service.KnowledgePointService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CourseAiServiceImpl implements CourseAiService {
    private final AgenticClient agenticClient;
    private final KnowledgePointService knowledgePointService;
    private final CourseResourceService courseResourceService;
    private final AbilityPointService abilityPointService;
    private final AbilityMapService abilityMapService;

    public CourseAiServiceImpl(AgenticClient agenticClient,
                               KnowledgePointService knowledgePointService,
                               CourseResourceService courseResourceService,
                               AbilityPointService abilityPointService,
                               AbilityMapService abilityMapService) {
        this.agenticClient = agenticClient;
        this.knowledgePointService = knowledgePointService;
        this.courseResourceService = courseResourceService;
        this.abilityPointService = abilityPointService;
        this.abilityMapService = abilityMapService;
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
    @Transactional
    public Result<AgenticResponse> generateAbilityMap(String courseCode) {
        List<KnowledgePoint> knowledgePoints = knowledgePointService.listByCourseCode(courseCode, null);
        if (knowledgePoints == null || knowledgePoints.isEmpty()) return Result.fail("请先为课程生成或维护知识点");
        AgenticRequest request = new AgenticRequest();
        request.setCourseCode(courseCode);
        request.setContent("""
                请把课程知识点归纳为 4-8 个能力点，并返回 JSON。
                每个能力点必须包含 name、description、knowledgePointIds。
                knowledgePointIds 只能使用输入知识点中的 knowledgePointId。
                """);
        request.setContext(Map.of("knowledgePoints", knowledgePoints));
        AgenticResponse response = agenticClient.invoke("ability-map", request);
        if (!response.isSuccess()) return Result.serviceUnavailable("AI 服务暂不可用");

        Map<String, Object> saveResult = saveGeneratedAbilityMap(courseCode, knowledgePoints, response.getData());
        Map<String, Object> data = new LinkedHashMap<>(response.getData());
        data.put("saved", saveResult);
        return Result.ok(new AgenticResponse(true, data, "能力图谱已生成"));
    }

    private Map<String, Object> saveGeneratedAbilityMap(String courseCode,
                                                       List<KnowledgePoint> knowledgePoints,
                                                       Map<String, Object> aiData) {
        List<Map<String, Object>> candidates = extractAbilityCandidates(aiData, knowledgePoints);
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("AI 未返回可保存的能力点");
        }

        Map<String, KnowledgePoint> pointById = knowledgePoints.stream()
                .filter(point -> point.getKnowledgePointId() != null)
                .collect(Collectors.toMap(KnowledgePoint::getKnowledgePointId, Function.identity(), (a, b) -> a));
        Map<String, KnowledgePoint> pointByName = knowledgePoints.stream()
                .filter(point -> point.getName() != null && !point.getName().isBlank())
                .collect(Collectors.toMap(point -> point.getName().trim(), Function.identity(), (a, b) -> a));
        Map<String, AbilityPoint> existingByName = abilityPointService.listByCourseCode(courseCode).stream()
                .filter(point -> point.getName() != null)
                .collect(Collectors.toMap(point -> point.getName().trim(), Function.identity(), (a, b) -> a));

        int created = 0;
        int reused = 0;
        int bound = 0;
        for (Map<String, Object> candidate : candidates) {
            String name = normalize(candidate.get("name"));
            if (name.isBlank()) continue;

            AbilityPoint abilityPoint = existingByName.get(name);
            if (abilityPoint == null) {
                abilityPoint = new AbilityPoint();
                abilityPoint.setCourseCode(courseCode);
                abilityPoint.setName(name);
                abilityPoint.setDescription(normalize(candidate.get("description")));
                abilityPointService.save(abilityPoint);
                existingByName.put(name, abilityPoint);
                created++;
            } else {
                reused++;
            }

            Set<String> knowledgePointIds = resolveKnowledgePointIds(candidate.get("knowledgePointIds"), pointById, pointByName);
            for (String knowledgePointId : knowledgePointIds) {
                if (abilityMapService.bindKnowledgePoint(abilityPoint.getAbilityPointId(), knowledgePointId)) bound++;
            }
        }

        return Map.of("createdAbilityPoints", created, "reusedAbilityPoints", reused, "createdMappings", bound);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractAbilityCandidates(Map<String, Object> aiData, List<KnowledgePoint> knowledgePoints) {
        Object abilityPoints = firstPresent(aiData, "abilityPoints", "abilities", "items", "data");
        if (abilityPoints instanceof List<?> list) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> raw) {
                    Map<String, Object> candidate = new LinkedHashMap<>();
                    raw.forEach((key, value) -> candidate.put(String.valueOf(key), value));
                    candidate.putIfAbsent("name", firstPresent(candidate, "title", "label"));
                    candidate.putIfAbsent("description", firstPresent(candidate, "summary", "desc"));
                    candidate.putIfAbsent("knowledgePointIds", firstPresent(candidate, "knowledge_points", "knowledgePoints", "kpIds"));
                    result.add(candidate);
                }
            }
            return result;
        }

        Object nodes = aiData.get("nodes");
        if (nodes instanceof List<?> list) {
            Map<String, KnowledgePoint> pointById = knowledgePoints.stream()
                    .filter(point -> point.getKnowledgePointId() != null)
                    .collect(Collectors.toMap(KnowledgePoint::getKnowledgePointId, Function.identity(), (a, b) -> a));
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> raw)) continue;
                String id = normalize(raw.get("id"));
                String name = normalize(raw.get("name"));
                if (name.isBlank()) continue;
                Map<String, Object> candidate = new LinkedHashMap<>();
                candidate.put("name", name);
                candidate.put("description", "AI 根据知识点生成的能力点");
                candidate.put("knowledgePointIds", pointById.containsKey(id) ? List.of(id) : List.of());
                result.add(candidate);
            }
            return result;
        }

        return List.of();
    }

    private Object firstPresent(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) return value;
        }
        return null;
    }

    private Set<String> resolveKnowledgePointIds(Object raw,
                                                 Map<String, KnowledgePoint> pointById,
                                                 Map<String, KnowledgePoint> pointByName) {
        Set<String> ids = new HashSet<>();
        if (raw instanceof List<?> list) {
            for (Object item : list) addKnowledgePointId(ids, item, pointById, pointByName);
        } else {
            addKnowledgePointId(ids, raw, pointById, pointByName);
        }
        return ids;
    }

    private void addKnowledgePointId(Set<String> ids,
                                     Object raw,
                                     Map<String, KnowledgePoint> pointById,
                                     Map<String, KnowledgePoint> pointByName) {
        if (raw == null) return;
        if (raw instanceof Map<?, ?> map) {
            Object value = map.get("knowledgePointId");
            if (value == null) value = map.get("id");
            if (value == null) value = map.get("name");
            addKnowledgePointId(ids, value, pointById, pointByName);
            return;
        }
        String value = normalize(raw);
        if (pointById.containsKey(value)) {
            ids.add(value);
            return;
        }
        KnowledgePoint byName = pointByName.get(value);
        if (byName != null) ids.add(byName.getKnowledgePointId());
    }

    private String normalize(Object value) {
        return Objects.toString(value, "").trim();
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
