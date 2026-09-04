package com.neu.CoursePlatform.service;

import com.neu.CoursePlatform.common.event.SubmissionAssessmentRequestedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 提交事务完成后异步触发评价，避免模型调用影响提交事务。 */
@Component
public class SubmissionAssessmentRequestedListener {

    private final SubmissionAiReviewService aiReviewService;

    public SubmissionAssessmentRequestedListener(SubmissionAiReviewService aiReviewService) {
        this.aiReviewService = aiReviewService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onRequested(SubmissionAssessmentRequestedEvent event) {
        if (event != null && event.submissionId() != null && !event.submissionId().isBlank()) {
            aiReviewService.requestAutomaticReview(event.submissionId());
        }
    }
}
