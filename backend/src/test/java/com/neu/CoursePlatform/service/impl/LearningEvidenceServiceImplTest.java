package com.neu.CoursePlatform.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neu.CoursePlatform.entity.KnowledgeMastery;
import com.neu.CoursePlatform.entity.KnowledgeMasteryHistory;
import com.neu.CoursePlatform.entity.LearningAnswerEvidence;
import com.neu.CoursePlatform.entity.Question;
import com.neu.CoursePlatform.mapper.KnowledgeMasteryHistoryMapper;
import com.neu.CoursePlatform.mapper.KnowledgeMasteryMapper;
import com.neu.CoursePlatform.mapper.LearningAnswerEvidenceMapper;
import com.neu.CoursePlatform.mapper.QuestionMapper;
import com.neu.CoursePlatform.profile.service.ProfileProjectionService;
import com.neu.CoursePlatform.service.LearningEvidenceService;
import com.neu.CoursePlatform.service.MasteryScoreCalculator;
import com.neu.CoursePlatform.service.QuestionAnswerEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningEvidenceServiceImplTest {
    @Mock private QuestionMapper questionMapper;
    @Mock private LearningAnswerEvidenceMapper evidenceMapper;
    @Mock private KnowledgeMasteryMapper masteryMapper;
    @Mock private KnowledgeMasteryHistoryMapper historyMapper;
    @Mock private ProfileProjectionService profileProjectionService;

    private LearningEvidenceServiceImpl service;
    private Question question;

    @BeforeEach
    void setUp() {
        service = new LearningEvidenceServiceImpl(questionMapper, evidenceMapper, masteryMapper, historyMapper,
                new QuestionAnswerEvaluator(new ObjectMapper()), new MasteryScoreCalculator(), profileProjectionService);
        question = new Question();
        question.setQuestionId("q-1");
        question.setCourseCode("c-1");
        question.setKnowledgePointId("kp-1");
        question.setType("single");
        question.setAnswer("A");
        question.setDifficulty(3);
        when(questionMapper.selectBatchIds(any())).thenReturn(List.of(question));
        when(masteryMapper.lockStudentForMasteryUpdate("s-1")).thenReturn("s-1");
        when(evidenceMapper.selectList(any())).thenReturn(List.of());
    }

    @Test
    void verifiedCorrectAnswerCreatesEvidenceAndProgressiveMastery() {
        when(evidenceMapper.insertIfAbsent(any())).thenReturn(1);

        LearningEvidenceService.BatchResult result = service.recordVerifiedAnswers(
                "s-1", "c-1", "evaluation-1", "battle_room",
                List.of(Map.of("questionId", "q-1", "studentAnswer", "A", "answered", true)), Set.of("q-1"));

        assertEquals(1, result.correctCount());
        assertEquals(57, result.answers().get(0).afterMastery());
        assertTrue(result.answers().get(0).applied());
        ArgumentCaptor<KnowledgeMastery> mastery = ArgumentCaptor.forClass(KnowledgeMastery.class);
        verify(masteryMapper).insert(mastery.capture());
        assertEquals(57, mastery.getValue().getMasteryScore());
        verify(evidenceMapper).insertIfAbsent(any(LearningAnswerEvidence.class));
        verify(historyMapper).insert(any(KnowledgeMasteryHistory.class));
        verify(profileProjectionService).applyAnswerEvidence(any(String.class));
    }

    @Test
    void replayedEvaluationDoesNotChangeMasteryAgain() {
        KnowledgeMastery current = new KnowledgeMastery();
        current.setMasteryScore(57);
        when(masteryMapper.selectOne(any())).thenReturn(current);
        when(evidenceMapper.insertIfAbsent(any())).thenReturn(0);

        LearningEvidenceService.BatchResult result = service.recordVerifiedAnswers(
                "s-1", "c-1", "evaluation-1", "battle_room",
                List.of(Map.of("questionId", "q-1", "studentAnswer", "A", "answered", true)), Set.of("q-1"));

        assertFalse(result.answers().get(0).applied());
        assertEquals(57, result.answers().get(0).beforeMastery());
        assertEquals(57, result.answers().get(0).afterMastery());
        verify(masteryMapper, never()).insert(any(KnowledgeMastery.class));
        verify(masteryMapper, never()).updateById(any(KnowledgeMastery.class));
        verify(historyMapper, never()).insert(any(KnowledgeMasteryHistory.class));
        verify(profileProjectionService, never()).applyAnswerEvidence(any(String.class));
    }

    @Test
    void unansweredObjectiveQuestionCountsAsGradedAndWrong() {
        when(evidenceMapper.insertIfAbsent(any())).thenReturn(1);

        LearningEvidenceService.BatchResult result = service.recordVerifiedAnswers(
                "s-1", "c-1", "evaluation-missing", "battle_room",
                List.of(Map.of("questionId", "q-1", "studentAnswer", "", "answered", false)), Set.of("q-1"));

        assertEquals(1, result.gradedCount());
        assertEquals(0, result.correctCount());
        assertEquals(0D, result.correctRate());
        assertFalse(result.answers().get(0).correct());
    }
}
