package com.neu.CoursePlatform.service.impl;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TowerRunAnswerValidationTest {
    private final TowerRunServiceImpl service = new TowerRunServiceImpl(
            null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null);

    @Test
    void fillsMissingPackQuestionsAsUnanswered() {
        Map<String, Object> request = Map.of("answerSummary", List.of(
                Map.of("questionId", "q1", "studentAnswer", "A", "correct", true)));

        List<Map<String, Object>> answers = service.answersForQuestionPack(request, Set.of("q1", "q2"));

        assertEquals(2, answers.size());
        Map<String, Object> missing = answers.stream()
                .filter(answer -> "q2".equals(answer.get("questionId"))).findFirst().orElseThrow();
        assertFalse(Boolean.TRUE.equals(missing.get("answered")));
        assertEquals("", missing.get("studentAnswer"));
    }

    @Test
    void rejectsDuplicateAndOutOfPackAnswers() {
        Map<String, Object> duplicate = Map.of("answerSummary", List.of(
                Map.of("questionId", "q1", "studentAnswer", "A"),
                Map.of("questionId", "q1", "studentAnswer", "A")));
        assertThrows(IllegalArgumentException.class,
                () -> service.answersForQuestionPack(duplicate, Set.of("q1", "q2")));

        Map<String, Object> foreign = Map.of("answerSummary", List.of(
                Map.of("questionId", "outside", "studentAnswer", "A")));
        assertThrows(IllegalArgumentException.class,
                () -> service.answersForQuestionPack(foreign, Set.of("q1", "q2")));
    }

    @Test
    void missingPackIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.allowedQuestionIds(
                Map.of(), "s1", null, null, Set.of("battle")));
    }
}
