package com.neu.CoursePlatform.service.impl;

import com.neu.CoursePlatform.entity.Question;
import com.neu.CoursePlatform.entity.StudentTowerNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TowerQuestionPackServiceImplTest {
    private final TowerQuestionPackServiceImpl service = new TowerQuestionPackServiceImpl(
            null, null, null, null, null, null, null);

    @Test
    void battleUsesOnlyCurrentKnowledgePoint() {
        StudentTowerNode node = node("kp-current", "battle");
        List<Question> candidates = new ArrayList<>();
        candidates.addAll(questions("kp-current", "current", 5));
        candidates.addAll(questions("kp-other", "other", 5));

        TowerQuestionPackServiceImpl.SelectionResult result = service.selectQuestions(
                candidates, node, "battle", Set.of(), Set.of(),
                List.of("kp-other"), TowerQuestionPackServiceImpl.WeaknessEvidence.empty(), 5);

        assertEquals(5, result.questions().size());
        assertTrue(result.questions().stream()
                .allMatch(question -> "kp-current".equals(question.getKnowledgePointId())));
        assertEquals("battle-current-kp-only", result.policy());
    }

    @Test
    void eliteUsesThreeCurrentTwoSameAbilityAndOneHistoricalWeakness() {
        StudentTowerNode node = node("kp-current", "elite");
        List<Question> candidates = new ArrayList<>();
        candidates.addAll(questions("kp-current", "current", 5));
        candidates.addAll(questions("kp-related", "related", 4));
        candidates.addAll(questions("kp-weak", "weak", 3));
        Set<String> weakIds = Set.of("weak-1");

        TowerQuestionPackServiceImpl.SelectionResult result = service.selectQuestions(
                candidates, node, "elite", Set.of(), Set.of(),
                List.of("kp-current", "kp-related"),
                new TowerQuestionPackServiceImpl.WeaknessEvidence(weakIds, Set.of("kp-weak")), 6);

        assertEquals(6, result.questions().size());
        assertEquals(3, result.currentCount());
        assertEquals(2, result.sameAbilityCount());
        assertEquals(1, result.weaknessCount());
        assertFalse(result.quotaFallback());
        assertEquals(3, result.questions().stream()
                .filter(question -> "kp-current".equals(question.getKnowledgePointId())).count());
        assertEquals(2, result.questions().stream()
                .filter(question -> "kp-related".equals(question.getKnowledgePointId())).count());
        assertEquals(1, result.questions().stream()
                .filter(question -> "kp-weak".equals(question.getKnowledgePointId())).count());
    }

    @Test
    void bossCanUseQuestionsAcrossKnowledgePoints() {
        StudentTowerNode node = node("kp-current", "boss");
        List<Question> candidates = new ArrayList<>();
        candidates.addAll(questions("kp-current", "current", 5));
        candidates.addAll(questions("kp-other", "other", 5));

        TowerQuestionPackServiceImpl.SelectionResult result = service.selectQuestions(
                candidates, node, "boss", Set.of(), Set.of(),
                List.of("kp-other"), TowerQuestionPackServiceImpl.WeaknessEvidence.empty(), 8);

        assertEquals(8, result.questions().size());
        assertTrue(result.questions().stream()
                .map(Question::getKnowledgePointId).distinct().count() > 1);
        assertEquals("boss-comprehensive-course", result.policy());
    }

    private StudentTowerNode node(String knowledgePointId, String roomType) {
        StudentTowerNode node = new StudentTowerNode();
        node.setNodeId("node-" + roomType);
        node.setKnowledgePointId(knowledgePointId);
        node.setRoomType(roomType);
        node.setDifficulty("boss".equals(roomType) ? 3 : 2);
        return node;
    }

    private List<Question> questions(String knowledgePointId, String prefix, int count) {
        List<Question> result = new ArrayList<>();
        for (int index = 1; index <= count; index++) {
            Question question = new Question();
            question.setQuestionId(prefix + "-" + index);
            question.setKnowledgePointId(knowledgePointId);
            question.setType(index % 2 == 0 ? "multi" : "single");
            question.setDifficulty(index % 5 + 1);
            result.add(question);
        }
        return result;
    }
}
