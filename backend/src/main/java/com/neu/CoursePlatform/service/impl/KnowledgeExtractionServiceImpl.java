package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neu.CoursePlatform.agentic.AgenticClient;
import com.neu.CoursePlatform.agentic.AgenticRequest;
import com.neu.CoursePlatform.agentic.AgenticResponse;
import com.neu.CoursePlatform.entity.CourseResource;
import com.neu.CoursePlatform.entity.KnowledgeExtractionCandidate;
import com.neu.CoursePlatform.entity.KnowledgePoint;
import com.neu.CoursePlatform.mapper.KnowledgeExtractionCandidateMapper;
import com.neu.CoursePlatform.service.CourseResourceService;
import com.neu.CoursePlatform.service.KnowledgeExtractionService;
import com.neu.CoursePlatform.service.KnowledgePointService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class KnowledgeExtractionServiceImpl implements KnowledgeExtractionService {

    private final AgenticClient agenticClient;
    private final CourseResourceService courseResourceService;
    private final KnowledgeExtractionCandidateMapper candidateMapper;
    private final KnowledgePointService knowledgePointService;

    public KnowledgeExtractionServiceImpl(AgenticClient agenticClient,
                                          CourseResourceService courseResourceService,
                                          KnowledgeExtractionCandidateMapper candidateMapper,
                                          KnowledgePointService knowledgePointService) {
        this.agenticClient = agenticClient;
        this.courseResourceService = courseResourceService;
        this.candidateMapper = candidateMapper;
        this.knowledgePointService = knowledgePointService;
    }

    @Override
    @Transactional
    public List<KnowledgeExtractionCandidate> extract(String courseCode, String resourceId) {
        CourseResource resource = courseResourceService.getById(resourceId);
        if (resource == null || !courseCode.equals(resource.getCourseCode())) {
            throw new IllegalArgumentException("课程资源不存在或不属于当前课程");
        }

        AgenticRequest request = new AgenticRequest();
        request.setCourseCode(courseCode);
        request.setResourceId(resourceId);
        request.setContent(resource.getTitle());
        request.setContext(Map.of(
                "resourceType", resource.getResourceType(),
                "originalFilename", safeValue(resource.getOriginalFilename()),
                "chapter", safeValue(resource.getChapter()),
                "instruction", "Return a knowledgePoints array; every item needs name and may include description, chapter and importance."
        ));

        AgenticResponse response = agenticClient.invoke("extract", request);
        if (!response.isSuccess()) throw new IllegalArgumentException(response.getMessage());

        List<KnowledgeExtractionCandidate> saved = new ArrayList<>();
        for (Map<String, Object> draft : readDrafts(response.getData())) {
            String name = stringValue(draft.get("name"));
            if (name == null || name.isBlank()) continue;
            KnowledgeExtractionCandidate candidate = new KnowledgeExtractionCandidate();
            candidate.setCourseCode(courseCode);
            candidate.setResourceId(resourceId);
            candidate.setName(name.trim());
            candidate.setDescription(stringValue(draft.get("description")));
            candidate.setChapter(defaultIfBlank(stringValue(draft.get("chapter")), resource.getChapter()));
            candidate.setImportance(readImportance(draft.get("importance")));
            candidate.setStatus("pending");
            candidate.setCreatedAt(LocalDateTime.now());
            candidateMapper.insert(candidate);
            saved.add(candidate);
        }
        return saved;
    }

    @Override
    public List<KnowledgeExtractionCandidate> listPending(String courseCode) {
        return candidateMapper.selectList(new LambdaQueryWrapper<KnowledgeExtractionCandidate>()
                .eq(KnowledgeExtractionCandidate::getCourseCode, courseCode)
                .eq(KnowledgeExtractionCandidate::getStatus, "pending")
                .orderByDesc(KnowledgeExtractionCandidate::getCreatedAt));
    }

    @Override
    @Transactional
    public String accept(String courseCode, String candidateId, KnowledgeExtractionCandidate editedCandidate) {
        KnowledgeExtractionCandidate candidate = getPendingCandidate(courseCode, candidateId);
        if (editedCandidate == null || editedCandidate.getName() == null || editedCandidate.getName().isBlank()) {
            return "知识点名称不能为空";
        }
        if (editedCandidate.getImportance() != null
                && (editedCandidate.getImportance() < 1 || editedCandidate.getImportance() > 5)) {
            return "知识点重要程度必须在 1 到 5 之间";
        }
        KnowledgePoint point = new KnowledgePoint();
        point.setCourseCode(courseCode);
        point.setName(editedCandidate.getName().trim());
        point.setDescription(editedCandidate.getDescription());
        point.setChapter(editedCandidate.getChapter());
        point.setImportance(editedCandidate.getImportance());
        point.setGenerationMethod("agentic-reviewed");
        knowledgePointService.save(point);

        candidate.setStatus("accepted");
        candidateMapper.updateById(candidate);
        return null;
    }

    @Override
    public String reject(String courseCode, String candidateId) {
        KnowledgeExtractionCandidate candidate = getPendingCandidate(courseCode, candidateId);
        candidate.setStatus("rejected");
        candidateMapper.updateById(candidate);
        return null;
    }

    private KnowledgeExtractionCandidate getPendingCandidate(String courseCode, String candidateId) {
        KnowledgeExtractionCandidate candidate = candidateMapper.selectById(candidateId);
        if (candidate == null || !courseCode.equals(candidate.getCourseCode()) || !"pending".equals(candidate.getStatus())) {
            throw new IllegalArgumentException("候选知识点不存在或已处理");
        }
        return candidate;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> readDrafts(Map<String, Object> data) {
        if (data == null) return List.of();
        Object raw = data.get("knowledgePoints");
        if (!(raw instanceof List<?>)) raw = data.get("candidates");
        if (!(raw instanceof List<?> items)) return List.of();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : items) {
            if (item instanceof Map<?, ?> map) {
                result.add((Map<String, Object>) map);
            }
        }
        return result;
    }

    private Integer readImportance(Object value) {
        if (value instanceof Number number) {
            int importance = number.intValue();
            return importance >= 1 && importance <= 5 ? importance : null;
        }
        try {
            int importance = Integer.parseInt(String.valueOf(value));
            return importance >= 1 && importance <= 5 ? importance : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String safeValue(String value) {
        return value == null ? "" : value;
    }
}
