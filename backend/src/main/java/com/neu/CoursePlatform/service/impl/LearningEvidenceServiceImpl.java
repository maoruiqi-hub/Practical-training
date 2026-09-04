package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neu.CoursePlatform.common.SharedIds;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LearningEvidenceServiceImpl implements LearningEvidenceService {
    private final QuestionMapper questionMapper;
    private final LearningAnswerEvidenceMapper evidenceMapper;
    private final KnowledgeMasteryMapper masteryMapper;
    private final KnowledgeMasteryHistoryMapper historyMapper;
    private final QuestionAnswerEvaluator answerEvaluator;
    private final MasteryScoreCalculator calculator;
    private final ProfileProjectionService profileProjectionService;

    public LearningEvidenceServiceImpl(QuestionMapper questionMapper,
                                       LearningAnswerEvidenceMapper evidenceMapper,
                                       KnowledgeMasteryMapper masteryMapper,
                                       KnowledgeMasteryHistoryMapper historyMapper,
                                       QuestionAnswerEvaluator answerEvaluator,
                                       MasteryScoreCalculator calculator,
                                       ProfileProjectionService profileProjectionService) {
        this.questionMapper = questionMapper;
        this.evidenceMapper = evidenceMapper;
        this.masteryMapper = masteryMapper;
        this.historyMapper = historyMapper;
        this.answerEvaluator = answerEvaluator;
        this.calculator = calculator;
        this.profileProjectionService = profileProjectionService;
    }

    @Override
    @Transactional
    public BatchResult recordVerifiedAnswers(String studentNo, String courseCode, String evaluationId,
                                             String sourceType, List<Map<String, Object>> answers,
                                             Set<String> allowedQuestionIds) {
        return recordAnswers(studentNo, courseCode, evaluationId, sourceType, answers, allowedQuestionIds, false);
    }

    @Override
    @Transactional
    public BatchResult recordReviewedAnswers(String studentNo, String courseCode, String evaluationId,
                                             List<Map<String, Object>> answers, Set<String> allowedQuestionIds) {
        return recordAnswers(studentNo, courseCode, evaluationId, "teacher_review", answers, allowedQuestionIds, true);
    }

    @Override
    @Transactional
    public BatchResult recordAiReviewedAnswers(String studentNo, String courseCode, String evaluationId,
                                               List<Map<String, Object>> answers, Set<String> allowedQuestionIds) {
        return recordAnswers(studentNo, courseCode, evaluationId, "ai_review", answers, allowedQuestionIds, true);
    }

    private BatchResult recordAnswers(String studentNo, String courseCode, String evaluationId,
                                      String sourceType, List<Map<String, Object>> answers,
                                      Set<String> allowedQuestionIds, boolean teacherReviewed) {
        List<String> questionIds = answers.stream()
                .map(answer -> stringValue(answer.get("questionId")))
                .filter(id -> !id.isBlank()).distinct().toList();
        if (questionIds.isEmpty()) return new BatchResult(List.of(), 0, 0, Set.of());

        Collection<Question> loaded = questionMapper.selectBatchIds(questionIds);
        Map<String, Question> questionIndex = loaded.stream()
                .collect(Collectors.toMap(Question::getQuestionId, Function.identity(), (a, b) -> a));
        validateQuestions(courseCode, questionIds, questionIndex, allowedQuestionIds);
        if (masteryMapper.lockStudentForMasteryUpdate(studentNo) == null) {
            throw new IllegalArgumentException("学生不存在");
        }

        Map<String, Integer> previousAttempts = new HashMap<>();
        evidenceMapper.selectList(new LambdaQueryWrapper<LearningAnswerEvidence>()
                        .eq(LearningAnswerEvidence::getStudentNo, studentNo)
                        .in(LearningAnswerEvidence::getQuestionId, questionIds))
                .forEach(item -> previousAttempts.merge(item.getQuestionId(), 1, Integer::sum));

        Map<String, KnowledgeMastery> masteryIndex = new HashMap<>();
        List<AnswerResult> results = new ArrayList<>();
        Set<String> affected = new LinkedHashSet<>();
        int correctCount = 0;
        int gradedCount = 0;

        for (Map<String, Object> answer : answers) {
            String questionId = stringValue(answer.get("questionId"));
            Question question = questionIndex.get(questionId);
            boolean eligible = teacherReviewed
                    ? question != null && !answerEvaluator.isAutoGradable(question)
                    : question != null && answerEvaluator.isAutoGradable(question);
            if (!eligible || (teacherReviewed && !isAnswered(answer))) continue;
            gradedCount++;
            boolean correct = teacherReviewed
                    ? Boolean.TRUE.equals(answer.get("correct"))
                    : isAnswered(answer) && answerEvaluator.isCorrect(question, answer.get("studentAnswer"));
            if (correct) correctCount++;
            int attemptNo = previousAttempts.getOrDefault(questionId, 0) + 1;
            String idempotencyKey = sourceType + ":" + evaluationId + ":" + questionId;

            LearningAnswerEvidence evidence = evidence(studentNo, courseCode, question, evaluationId,
                    sourceType, answer, correct, attemptNo, idempotencyKey);
            if (evidenceMapper.insertIfAbsent(evidence) == 0) {
                KnowledgeMastery current = currentMastery(studentNo, courseCode, question.getKnowledgePointId(), masteryIndex);
                int score = current == null || current.getMasteryScore() == null ? 50 : current.getMasteryScore();
                results.add(new AnswerResult(questionId, question.getKnowledgePointId(), correct,
                        Math.max(1, attemptNo - 1), score, score, false));
                continue;
            }

            previousAttempts.put(questionId, attemptNo);
            KnowledgeMastery mastery = currentMastery(studentNo, courseCode, question.getKnowledgePointId(), masteryIndex);
            int oldScore = mastery == null || mastery.getMasteryScore() == null ? 50 : mastery.getMasteryScore();
            MasteryScoreCalculator.Calculation calculation = calculator.calculate(
                    oldScore, question.getDifficulty() == null ? 1 : question.getDifficulty(), attemptNo, correct);
            mastery = saveMastery(mastery, studentNo, courseCode, question.getKnowledgePointId(), evaluationId, calculation.afterScore());
            masteryIndex.put(question.getKnowledgePointId(), mastery);
            historyMapper.insert(history(evidence, calculation));
            profileProjectionService.applyAnswerEvidence(evidence.getEvidenceId());
            affected.add(question.getKnowledgePointId());
            results.add(new AnswerResult(questionId, question.getKnowledgePointId(), correct, attemptNo,
                    calculation.beforeScore(), calculation.afterScore(), true));
        }
        return new BatchResult(List.copyOf(results), correctCount, gradedCount, Set.copyOf(affected));
    }

    private void validateQuestions(String courseCode, List<String> questionIds, Map<String, Question> questionIndex,
                                   Set<String> allowedQuestionIds) {
        for (String questionId : questionIds) {
            Question question = questionIndex.get(questionId);
            if (question == null) throw new IllegalArgumentException("题目不存在：" + questionId);
            if (!courseCode.equals(question.getCourseCode())) throw new IllegalArgumentException("题目不属于当前课程");
            if (allowedQuestionIds != null && !allowedQuestionIds.isEmpty() && !allowedQuestionIds.contains(questionId)) {
                throw new IllegalArgumentException("题目不属于当前题包");
            }
            if (question.getKnowledgePointId() == null || question.getKnowledgePointId().isBlank()) {
                throw new IllegalArgumentException("题目未关联知识点：" + questionId);
            }
        }
    }

    private LearningAnswerEvidence evidence(String studentNo, String courseCode, Question question,
                                            String evaluationId, String sourceType, Map<String, Object> answer,
                                            boolean correct, int attemptNo, String idempotencyKey) {
        LearningAnswerEvidence evidence = new LearningAnswerEvidence();
        evidence.setEvidenceId(SharedIds.newId());
        evidence.setStudentNo(studentNo);
        evidence.setCourseCode(courseCode);
        evidence.setQuestionId(question.getQuestionId());
        evidence.setKnowledgePointId(question.getKnowledgePointId());
        evidence.setDifficulty(question.getDifficulty() == null ? 1 : question.getDifficulty());
        evidence.setAttemptNo(attemptNo);
        evidence.setFirstAttempt(attemptNo == 1);
        evidence.setCorrect(correct);
        evidence.setAnswerContent(stringValue(answer.get("studentAnswer")));
        evidence.setSourceType(sourceType);
        evidence.setSourceId(evaluationId);
        evidence.setIdempotencyKey(idempotencyKey);
        evidence.setFormulaVersion(MasteryScoreCalculator.FORMULA_VERSION);
        evidence.setAnsweredAt(LocalDateTime.now());
        return evidence;
    }

    private KnowledgeMastery currentMastery(String studentNo, String courseCode, String knowledgePointId,
                                             Map<String, KnowledgeMastery> cache) {
        if (cache.containsKey(knowledgePointId)) return cache.get(knowledgePointId);
        KnowledgeMastery mastery = masteryMapper.selectOne(new LambdaQueryWrapper<KnowledgeMastery>()
                .eq(KnowledgeMastery::getStudentNo, studentNo)
                .eq(KnowledgeMastery::getCourseCode, courseCode)
                .eq(KnowledgeMastery::getKnowledgePointId, knowledgePointId));
        cache.put(knowledgePointId, mastery);
        return mastery;
    }

    private KnowledgeMastery saveMastery(KnowledgeMastery mastery, String studentNo, String courseCode,
                                          String knowledgePointId, String sourceId, int score) {
        boolean create = mastery == null;
        if (create) {
            mastery = new KnowledgeMastery();
            mastery.setMasteryId(SharedIds.newId());
            mastery.setStudentNo(studentNo);
            mastery.setCourseCode(courseCode);
            mastery.setKnowledgePointId(knowledgePointId);
        }
        mastery.setMasteryScore(score);
        mastery.setSourceType("assessment");
        mastery.setSourceId(sourceId);
        mastery.setUpdatedAt(LocalDateTime.now());
        if (create) masteryMapper.insert(mastery); else masteryMapper.updateById(mastery);
        return mastery;
    }

    private KnowledgeMasteryHistory history(LearningAnswerEvidence evidence, MasteryScoreCalculator.Calculation calculation) {
        KnowledgeMasteryHistory history = new KnowledgeMasteryHistory();
        history.setHistoryId(SharedIds.newId());
        history.setEvidenceId(evidence.getEvidenceId());
        history.setStudentNo(evidence.getStudentNo());
        history.setCourseCode(evidence.getCourseCode());
        history.setKnowledgePointId(evidence.getKnowledgePointId());
        history.setBeforeScore(calculation.beforeScore());
        history.setAfterScore(calculation.afterScore());
        history.setTargetScore(calculation.targetScore());
        history.setAlpha(calculation.alpha());
        history.setFormulaVersion(MasteryScoreCalculator.FORMULA_VERSION);
        history.setCreatedAt(LocalDateTime.now());
        return history;
    }

    private boolean isAnswered(Map<String, Object> answer) {
        if (answer.containsKey("answered")) return Boolean.TRUE.equals(answer.get("answered"));
        return !stringValue(answer.get("studentAnswer")).isBlank();
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
