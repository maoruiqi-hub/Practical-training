package com.neu.CoursePlatform.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface LearningEvidenceService {
    BatchResult recordVerifiedAnswers(String studentNo, String courseCode, String evaluationId,
                                      String sourceType, List<Map<String, Object>> answers,
                                      Set<String> allowedQuestionIds);

    BatchResult recordReviewedAnswers(String studentNo, String courseCode, String evaluationId,
                                      List<Map<String, Object>> answers, Set<String> allowedQuestionIds);

    /** AI 评价通过可信度策略后的主观/编程题证据。 */
    BatchResult recordAiReviewedAnswers(String studentNo, String courseCode, String evaluationId,
                                       List<Map<String, Object>> answers, Set<String> allowedQuestionIds);

    record AnswerResult(String questionId, String knowledgePointId, boolean correct, int attemptNo,
                        int beforeMastery, int afterMastery, boolean applied) {}

    record BatchResult(List<AnswerResult> answers, int correctCount, int gradedCount,
                       Set<String> affectedKnowledgePointIds) {
        public double correctRate() {
            return gradedCount <= 0 ? 0D : correctCount / (double) gradedCount;
        }
    }
}
