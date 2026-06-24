package com.neu.CoursePlatform.service.impl;

import com.neu.CoursePlatform.common.game.GameEventPublisher;
import com.neu.CoursePlatform.common.game.TowerGameEvent;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.entity.SubmissionAnswer;
import com.neu.CoursePlatform.service.CourseGameConfigService;
import com.neu.CoursePlatform.service.TowerAssessmentEventService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TowerAssessmentEventServiceImpl implements TowerAssessmentEventService {

    private final CourseGameConfigService gameConfigService;
    private final GameEventPublisher gameEventPublisher;

    public TowerAssessmentEventServiceImpl(CourseGameConfigService gameConfigService,
                                           GameEventPublisher gameEventPublisher) {
        this.gameConfigService = gameConfigService;
        this.gameEventPublisher = gameEventPublisher;
    }

    @Override
    public void publishAssessmentEvents(LearningTask task, String studentNo, String submissionId,
                                        List<SubmissionAnswer> answers) {
        if (task == null || !gameConfigService.isGameModeEnabled(task.getCourseCode())) return;
        List<SubmissionAnswer> objectiveAnswers = answers.stream()
                .filter(answer -> Boolean.TRUE.equals(answer.getAutoGradable()))
                .filter(answer -> answer.getKnowledgePointId() != null && !answer.getKnowledgePointId().isBlank())
                .toList();
        for (SubmissionAnswer answer : objectiveAnswers) {
            String eventType = Boolean.TRUE.equals(answer.getCorrect()) ? "answer_correct" : "answer_wrong";
            publish(eventType, task, studentNo, submissionId, Map.of(
                    "question_id", answer.getQuestionId(),
                    "knowledge_point_id", answer.getKnowledgePointId(),
                    "difficulty", 3,
                    "is_first_attempt", true,
                    "attempt_count", 1));
        }
        for (Map.Entry<String, List<SubmissionAnswer>> group : objectiveAnswers.stream()
                .collect(Collectors.groupingBy(SubmissionAnswer::getKnowledgePointId)).entrySet()) {
            double correctRate = group.getValue().stream().filter(answer -> Boolean.TRUE.equals(answer.getCorrect())).count()
                    * 100.0 / group.getValue().size();
            Map<String, Object> payload = new HashMap<>();
            payload.put("knowledge_point_id", group.getKey());
            payload.put("correct_rate", correctRate);
            payload.put("is_perfect", correctRate == 100.0);
            payload.put("time_total_ms", 0L);
            if (correctRate >= 85) publish("floor_cleared", task, studentNo, submissionId, payload);
            else if (correctRate < 40) publish("floor_failed", task, studentNo, submissionId, payload);
        }
    }

    private void publish(String eventType, LearningTask task, String studentNo, String submissionId,
                         Map<String, Object> payload) {
        gameEventPublisher.publish(new TowerGameEvent(UUID.randomUUID().toString(), eventType, studentNo,
                task.getCourseCode(), "module3_assessment", submissionId, LocalDateTime.now(), payload));
    }
}
