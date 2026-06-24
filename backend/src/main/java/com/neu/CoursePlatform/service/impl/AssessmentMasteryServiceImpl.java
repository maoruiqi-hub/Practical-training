package com.neu.CoursePlatform.service.impl;

import com.neu.CoursePlatform.dto.KnowledgeMasteryUpdateRequest;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.entity.SubmissionAnswer;
import com.neu.CoursePlatform.service.AssessmentMasteryService;
import com.neu.CoursePlatform.service.KnowledgeMasteryService;
import com.neu.CoursePlatform.service.LearningTaskService;
import com.neu.CoursePlatform.service.SubmissionAnswerService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AssessmentMasteryServiceImpl implements AssessmentMasteryService {

    private final SubmissionAnswerService submissionAnswerService;
    private final LearningTaskService taskService;
    private final KnowledgeMasteryService knowledgeMasteryService;

    public AssessmentMasteryServiceImpl(SubmissionAnswerService submissionAnswerService,
                                        LearningTaskService taskService,
                                        KnowledgeMasteryService knowledgeMasteryService) {
        this.submissionAnswerService = submissionAnswerService;
        this.taskService = taskService;
        this.knowledgeMasteryService = knowledgeMasteryService;
    }

    @Override
    public void refreshFromObjectiveAnswers(String studentNo, String courseCode,
                                            List<SubmissionAnswer> latestAnswers, String sourceId) {
        Set<String> knowledgePointIds = latestAnswers.stream()
                .map(SubmissionAnswer::getKnowledgePointId)
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toSet());
        for (String knowledgePointId : knowledgePointIds) {
            List<SubmissionAnswer> evidence = submissionAnswerService
                    .listByStudentNo(studentNo, null, knowledgePointId, null).stream()
                    .filter(answer -> Boolean.TRUE.equals(answer.getAutoGradable()))
                    .filter(answer -> belongsToCourse(answer.getTaskNo(), courseCode))
                    .toList();
            if (evidence.isEmpty()) continue;
            int score = (int) Math.round(evidence.stream()
                    .filter(answer -> Boolean.TRUE.equals(answer.getCorrect()))
                    .count() * 100.0 / evidence.size());
            KnowledgeMasteryUpdateRequest request = new KnowledgeMasteryUpdateRequest();
            request.setStudentNo(studentNo);
            request.setCourseCode(courseCode);
            request.setKnowledgePointId(knowledgePointId);
            request.setMasteryScore(score);
            request.setSourceType("assessment");
            request.setSourceId(sourceId);
            knowledgeMasteryService.upsert(request);
        }
    }

    private boolean belongsToCourse(String taskNo, String courseCode) {
        LearningTask task = taskService.getById(taskNo);
        return task != null && courseCode.equals(task.getCourseCode());
    }
}
